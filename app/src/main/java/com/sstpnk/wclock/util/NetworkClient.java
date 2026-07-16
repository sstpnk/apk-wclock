package com.sstpnk.wclock.util;

import android.os.Build;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public final class NetworkClient {
    private final String userAgent;
    private final int timeoutMillis;

    public NetworkClient(String userAgent, int timeoutMillis) {
        this.userAgent = userAgent;
        this.timeoutMillis = timeoutMillis;
    }

    public String get(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        enableModernTls(connection);
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = readAll(stream);
            if (code < 200 || code >= 300) {
                throw new RuntimeException("HTTP " + code + ": " + body);
            }
            return body;
        } finally {
            connection.disconnect();
        }
    }

    private void enableModernTls(HttpURLConnection connection) throws Exception {
        if (!(connection instanceof HttpsURLConnection) || Build.VERSION.SDK_INT > 20) {
            return;
        }
        HttpsURLConnection https = (HttpsURLConnection) connection;
        https.setSSLSocketFactory(new Tls12SocketFactory(legacySslSocketFactory()));
    }

    private static SSLSocketFactory legacySslSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{
                new MergedTrustManager(defaultTrustManager(), additionalCaTrustManager(LEGACY_WEATHER_CA_PEM))
        }, null);
        return sslContext.getSocketFactory();
    }

    private static X509TrustManager defaultTrustManager() throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        return findX509TrustManager(factory.getTrustManagers());
    }

    static X509TrustManager additionalCaTrustManagerForTest(String pem) throws Exception {
        return additionalCaTrustManager(pem);
    }

    private static X509TrustManager additionalCaTrustManager(String pem) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream input = new ByteArrayInputStream(pem.getBytes("US-ASCII"));
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(input);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        int index = 0;
        for (Certificate certificate : certificates) {
            keyStore.setCertificateEntry("legacy-weather-ca-" + index, certificate);
            index++;
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        return findX509TrustManager(factory.getTrustManagers());
    }

    private static X509TrustManager findX509TrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new IllegalStateException("No X509TrustManager");
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    private static final class Tls12SocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        Tls12SocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws java.io.IOException {
            return enableTls12(delegate.createSocket(socket, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws java.io.IOException {
            return enableTls12(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws java.io.IOException {
            return enableTls12(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws java.io.IOException {
            return enableTls12(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws java.io.IOException {
            return enableTls12(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket enableTls12(Socket socket) {
            if (socket instanceof SSLSocket) {
                SSLSocket sslSocket = (SSLSocket) socket;
                if (Arrays.asList(sslSocket.getSupportedProtocols()).contains("TLSv1.2")) {
                    sslSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
                }
            }
            return socket;
        }
    }

    private static final class MergedTrustManager implements X509TrustManager {
        private final X509TrustManager primary;
        private final X509TrustManager fallback;

        MergedTrustManager(X509TrustManager primary, X509TrustManager fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            primary.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            try {
                primary.checkServerTrusted(chain, authType);
            } catch (CertificateException primaryError) {
                fallback.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            java.security.cert.X509Certificate[] primaryIssuers = primary.getAcceptedIssuers();
            java.security.cert.X509Certificate[] fallbackIssuers = fallback.getAcceptedIssuers();
            java.security.cert.X509Certificate[] issuers = Arrays.copyOf(primaryIssuers, primaryIssuers.length + fallbackIssuers.length);
            System.arraycopy(fallbackIssuers, 0, issuers, primaryIssuers.length, fallbackIssuers.length);
            return issuers;
        }
    }

    private static final String LEGACY_WEATHER_CA_PEM =
            "-----BEGIN CERTIFICATE-----\n"
                    + "MIIGBTCCA+2gAwIBAgIQFNV782kiKCGaVWf6kWUbIjANBgkqhkiG9w0BAQsFADBsMQswCQYDVQQG\n"
                    + "EwJHUjE3MDUGA1UECgwuSGVsbGVuaWMgQWNhZGVtaWMgYW5kIFJlc2VhcmNoIEluc3RpdHV0aW9u\n"
                    + "cyBDQTEkMCIGA1UEAwwbSEFSSUNBIFRMUyBSU0EgUm9vdCBDQSAyMDIxMB4XDTI1MDEwMzExMTUw\n"
                    + "MFoXDTM5MTIzMTExMTQ1OVowYDELMAkGA1UEBhMCR1IxNzA1BgNVBAoMLkhlbGxlbmljIEFjYWRl\n"
                    + "bWljIGFuZCBSZXNlYXJjaCBJbnN0aXR1dGlvbnMgQ0ExGDAWBgNVBAMMD0dFQU5UIFRMUyBSU0Eg\n"
                    + "MTCCAaIwDQYJKoZIhvcNAQEBBQADggGPADCCAYoCggGBAKEEaZSzEzznAPk8IEa17GSGyJzPTj4c\n"
                    + "wRY7/vcq2BPT5+IRGxQtaCdgLXIEl2cdPdIkj2eyakFmgMjAtyeju8V8dRayQCD/bWjJ7thDlowg\n"
                    + "LljQaXirxnYbT8bzRHAhCZqBakYgi5KWw9dANLyDHGpXUdY259ab0lWEaFE5Uu6IzQSMJOAy4l/T\n"
                    + "wym8GUiy0qMDEBFSlm31C9BXpdHKKAlhvIjMiKoDeTWl5vZaLB2MMRGY1yW2ftPgIP0/MkX1uFIT\n"
                    + "lvHmmMTngxplH1nybEIJFiwHg1KiLk1TprcZgeO2gxE5Lz3wTFWrsUlAzrh5xWmscWkjNi/4Bpeu\n"
                    + "iT5+NExFczboLnXOfjuci/7bsnPi1/aZN/iKNbJRnngFoLaKVMmqCS7Xo34f+BITatryQZFEu2oD\n"
                    + "KExQGlxDBCfYMLgLucX/onpLzUSgeQITNLx6i5tGGbUYH+9Dy3GI66L/5tPjqzlOsydki8ZYGE5S\n"
                    + "BJeWCZ2IrhUe0WzZ2b6Zhk6JAQIDAQABo4IBLTCCASkwEgYDVR0TAQH/BAgwBgEB/wIBADAfBgNV\n"
                    + "HSMEGDAWgBQKSCOmYKSSCjPqk1vFV+olTb0S7jBNBggrBgEFBQcBAQRBMD8wPQYIKwYBBQUHMAKG\n"
                    + "MWh0dHA6Ly9jcnQuaGFyaWNhLmdyL0hBUklDQS1UTFMtUm9vdC0yMDIxLVJTQS5jZXIwEQYDVR0g\n"
                    + "BAowCDAGBgRVHSAAMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATBCBgNVHR8EOzA5MDeg\n"
                    + "NaAzhjFodHRwOi8vY3JsLmhhcmljYS5nci9IQVJJQ0EtVExTLVJvb3QtMjAyMS1SU0EuY3JsMB0G\n"
                    + "A1UdDgQWBBSGAXI/jKlw4jEGUxbOAV9becg8OzAOBgNVHQ8BAf8EBAMCAYYwDQYJKoZIhvcNAQEL\n"
                    + "BQADggIBABkssjQzYrOo4GMsKegaChP16yNe6SckcWBymM455R2rMeuQ3zlxUNOEt+KUfgueOA2u\n"
                    + "rp4j6TlPbs/XxpwuN3I1f09Luk5b+ZgRXM7obE6ZLTerVQWKoTShyl34R2XlK8pEy7+67Ht4lcJz\n"
                    + "t+K6K5gEuoPSGQDPef+fUfmXrFcgBMcMbtfDb9dubFKNZZxo5nAXiqhFMOIyByag3H+tOTuH8zuI\n"
                    + "d9pHRDsUpAIHJ9/W2WBfLcKav7IKRlNBRD/sPBy903J9WHPKwl8kQSDA+aa7XCYk7bJtEyf+7GM9\n"
                    + "F5cZ7+YyknXqnv/rtQEkTKZdQo5Us18VFe9qqj94tXbLdk7PejJYNB4OZlli44Ld7rtqfFlUych7\n"
                    + "gIxFOmiyxMQQYrYmUi+74lEZvfoNhuref0CupuKpz6O3dLv6kO9T10uNdDBoBQTkge3UzHafTIe3\n"
                    + "R2o3ujXKUGPwyc9m7/FETyKLUCwSU/5OAVOeBCU8QtkKKjM8AmbpKpe3pHWcyq3R7B3LmIALkMPT\n"
                    + "ydyDfxen65IDqREbVq8NxjhkJThUz40JqOlN6uqKqeDISj/IoucYwsqW24AlO7ZzNmohQmMi8ep2\n"
                    + "3H4hBSh0GBTe2XvkuzaNf92syK8l2HzO+13GLCjzYLTPvXTO9UpK8DGyfGZOuamuwbAnbNpE3Rfj\n"
                    + "V9IaUQGJ\n"
                    + "-----END CERTIFICATE-----\n"
                    + "-----BEGIN CERTIFICATE-----\n"
                    + "MIIFpDCCA4ygAwIBAgIQOcqTHO9D88aOk8f0ZIk4fjANBgkqhkiG9w0BAQsFADBsMQswCQYDVQQG\n"
                    + "EwJHUjE3MDUGA1UECgwuSGVsbGVuaWMgQWNhZGVtaWMgYW5kIFJlc2VhcmNoIEluc3RpdHV0aW9u\n"
                    + "cyBDQTEkMCIGA1UEAwwbSEFSSUNBIFRMUyBSU0EgUm9vdCBDQSAyMDIxMB4XDTIxMDIxOTEwNTUz\n"
                    + "OFoXDTQ1MDIxMzEwNTUzN1owbDELMAkGA1UEBhMCR1IxNzA1BgNVBAoMLkhlbGxlbmljIEFjYWRl\n"
                    + "bWljIGFuZCBSZXNlYXJjaCBJbnN0aXR1dGlvbnMgQ0ExJDAiBgNVBAMMG0hBUklDQSBUTFMgUlNB\n"
                    + "IFJvb3QgQ0EgMjAyMTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAIvC569lmwVnlskN\n"
                    + "JLnQDmT8zuIkGCyEf3dRywQRNrhe7Wlxp57kJQmXZ8FHws+RFjZiPTgE4VGC/6zStGndLuwRo0Xu\n"
                    + "a2s7TL+MjaQenRG56Tj5eg4MmOIjHdFOY9TnuEFE+2uva9of08WRiFukiZLRgeaMOVig1mlDqa2Y\n"
                    + "Ulhu2wr7a89o+uOkXjpFc5gH6l8Cct4MpbOfrqkdtx2z/IpZ525yZa31MJQjB/OCFks1mJxTuy/K\n"
                    + "5FrZx40d/JiZ+yykgmvwKh+OC19xXFyuQnspiYHLA6OZyoieC0AJQTPb5lh6/a6ZcMBaD9YThnEv\n"
                    + "dmn8kN3bLW7R8pv1GmuebxWMevBLKKAiOIAkbDakO/IwkfN4E8/BPzWr8R0RI7VDIp4BkrcYAuUR\n"
                    + "0YLbFQDMYTfBKnya4dC6s1BG7oKsnTH4+yPiAwBIcKMJJnkVU2DzOFytOOqBAGMUuTNe3QvboEUH\n"
                    + "GjMJ+E20pwKmafTCWQWIZYVWrkvL4N48fS0ayOn7H6NhStYqE613TBoYm5EPWNgGVMWX+Ko/IIqm\n"
                    + "haZ39qb8HOLubpQzKoNQhArlT4b4UEV4AIHrW2jjJo3Me1xR9BQsQL4aYB16cmEdH2MtiKrOokWQ\n"
                    + "CPxrvrNQKlr9qEgYRtaQQJKQCoReaDH46+0N0x3GfZkYVVYnZS6NRcUk7M7jAgMBAAGjQjBAMA8G\n"
                    + "A1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFApII6ZgpJIKM+qTW8VX6iVNvRLuMA4GA1UdDwEB/wQE\n"
                    + "AwIBhjANBgkqhkiG9w0BAQsFAAOCAgEAPpBIqm5iFSVmewzVjIuJndftTgfvnNAUX15QvWiWkKQU\n"
                    + "EapobQk1OUAJ2vQJLDSle1mESSmXdMgHHkdt8s4cUCbjnj1AUz/3f5Z2EMVGpdAgS1D0NTsY9FVq\n"
                    + "QRtHBmg8uwkIYtlfVUKqrFOFrJVWNlar5AWMxajaH6NpvVMPxP/cyuN+8kyIhkdGGvMA9YCRotxD\n"
                    + "QpSbIPDRzbLrLFPCU3hKTwSUQZqPJzLB5UkZv/HywouoCjkxKLR9YjYsTewfM7Z+d21+UPCfDtcR\n"
                    + "j88YxeMn/ibvBZ3PzzfF0HvaO7AWhAw6k9a+F9sPPg4ZeAnHqQJyIkv3N3a6dcSFA1pj1bF1BcK5\n"
                    + "vZStjBWZp5N99sXzqnTPBIWUmAD04vnKJGW/4GKvyMX6ssmeVkjaef2WdhW+o45WxLM0/L5H9MG0\n"
                    + "qPzVMIho7suuyWPEdr6sOBjhXlzPrjoiUevRi7PzKzMHVIf6tLITe7pTBGIBnfHAT+7hOtSLIBD6\n"
                    + "Alfm78ELt5BGnBkpjNxvoEppaZS3JGWg/6w/zgH7IS79aPib8qXPMThcFarmlwDB31qlpzmq6YR/\n"
                    + "PFGoOtmUW4y/Twhx5duoXNTSpv4Ao8YWxw/ogM4cKGR0GQjTQuPOAF1/sdwTsOEFy9EgqoZ0njnn\n"
                    + "kf3/W9b3raYvAwtt41dU63ZTGI0RmLo=\n"
                    + "-----END CERTIFICATE-----\n";
}
