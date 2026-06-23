package com.sstpnk.wclock.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FileBrowserActivity extends Activity {
    private File current;
    private TextView path;
    private ArrayAdapter<String> adapter;
    private final List<File> entries = new ArrayList<File>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current = Environment.getExternalStorageDirectory();
        setContentView(buildContent());
        load(current);
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        path = new TextView(this);
        path.setTextSize(18);
        root.addView(path);

        Button choose = new Button(this);
        choose.setText("Выбрать эту папку");
        choose.setFocusable(true);
        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                data.putExtra("path", current.getAbsolutePath());
                setResult(RESULT_OK, data);
                finish();
            }
        });
        root.addView(choose);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selected = entries.get(position);
                if ("..".equals(selected.getName())) {
                    File parentFile = current.getParentFile();
                    load(parentFile == null ? current : parentFile);
                } else if (selected.isDirectory()) {
                    load(selected);
                }
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void load(File dir) {
        current = dir;
        path.setText(dir.getAbsolutePath());
        entries.clear();
        List<String> names = new ArrayList<String>();
        if (dir.getParentFile() != null) {
            entries.add(new File(".."));
            names.add("..");
        }
        File[] files = dir.listFiles();
        if (files != null) {
            List<File> sorted = Arrays.asList(files);
            Collections.sort(sorted);
            for (File file : sorted) {
                if (file.isDirectory() && !file.isHidden()) {
                    entries.add(file);
                    names.add(file.getName() + "/");
                }
            }
        }
        adapter.clear();
        adapter.addAll(names);
        adapter.notifyDataSetChanged();
    }
}
