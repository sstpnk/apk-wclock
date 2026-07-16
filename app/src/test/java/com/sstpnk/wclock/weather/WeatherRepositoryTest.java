package com.sstpnk.wclock.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WeatherRepositoryTest {
    @Test
    public void usesFallbackWhenPrimaryFails() {
        WeatherProvider failing = new FakeProvider("primary", true, 1.0);
        WeatherProvider fallback = new FakeProvider("fallback", false, 2.0);
        WeatherRepository repository = new WeatherRepository(null, failing, fallback);

        WeatherData data = repository.refreshForTest("Москва", 55.7, 37.6, 1000L);

        assertEquals("fallback", data.providerName);
        assertEquals(2.0, data.temperatureC, 0.01);
    }

    @Test
    public void usesSecondFallbackWhenHttpsProvidersFail() {
        WeatherProvider failingPrimary = new FakeProvider("primary", true, 1.0);
        WeatherProvider failingFallback = new FakeProvider("fallback", true, 2.0);
        WeatherProvider httpFallback = new FakeProvider("http", false, 3.0);
        WeatherRepository repository = new WeatherRepository(null, failingPrimary, failingFallback, httpFallback);

        WeatherData data = repository.refreshForTest("РњРѕСЃРєРІР°", 55.7, 37.6, 1000L);

        assertEquals("http", data.providerName);
        assertEquals(3.0, data.temperatureC, 0.01);
        assertEquals("primary: fail; fallback: fail; http: OK", repository.lastDiagnostics());
    }

    @Test
    public void diagnosticsIncludeEveryProviderFailureWhenAllFail() {
        WeatherProvider failingPrimary = new FakeProvider("primary", true, 1.0);
        WeatherProvider failingFallback = new FakeProvider("fallback", true, 2.0);
        WeatherProvider failingHttp = new FakeProvider("http", true, 3.0);
        WeatherRepository repository = new WeatherRepository(null, failingPrimary, failingFallback, failingHttp);

        repository.refreshForTest("РњРѕСЃРєРІР°", 55.7, 37.6, 1000L);

        assertEquals("primary: fail; fallback: fail; http: fail", repository.lastDiagnostics());
    }

    @Test
    public void diagnosticsIncludeExceptionClassWhenMessageIsEmpty() {
        WeatherProvider emptyMessage = new EmptyMessageProvider();
        WeatherRepository repository = new WeatherRepository(null, emptyMessage, new FakeProvider("fallback", false, 2.0));

        repository.refreshForTest("РњРѕСЃРєРІР°", 55.7, 37.6, 1000L);

        assertEquals("empty: RuntimeException; fallback: OK", repository.lastDiagnostics());
    }

    private static final class FakeProvider implements WeatherProvider {
        private final String name;
        private final boolean fail;
        private final double temp;

        FakeProvider(String name, boolean fail, double temp) {
            this.name = name;
            this.fail = fail;
            this.temp = temp;
        }

        public String name() {
            return name;
        }

        public String buildUrl(double latitude, double longitude) {
            return "memory://" + name;
        }

        public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
            if (fail) {
                throw new RuntimeException("fail");
            }
            return new WeatherData(name, cityName, updatedAtMillis, false, temp, 0, "Ясно", java.util.Collections.<ForecastDay>emptyList());
        }
    }

    private static final class EmptyMessageProvider implements WeatherProvider {
        public String name() {
            return "empty";
        }

        public String buildUrl(double latitude, double longitude) {
            return "memory://empty";
        }

        public WeatherData parse(String cityName, String body, long updatedAtMillis) {
            throw new RuntimeException();
        }
    }
}
