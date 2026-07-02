package com.sstpnk.wclock.settings;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SettingsActivityTest {
    @Test
    public void settingsUseCompactSpinnersAndColumns() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();

        assertTrue("Provider, location and refresh controls should use dropdown spinners", countViews(activity.getWindow().getDecorView(), Spinner.class) >= 3);
        assertTrue("Settings should use horizontal groups to reduce one-long-column scrolling", countHorizontalRows(activity.getWindow().getDecorView()) >= 4);
        assertRowHasColumns(activity, SettingsActivity.TAG_LOCATION_COORDINATES_ROW, 2);
        assertRowHasColumns(activity, SettingsActivity.TAG_BRIGHTNESS_ROW, 2);
    }

    @Test
    public void settingsTextIsReadableRussianWithoutMojibake() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        String text = collectText(activity.getWindow().getDecorView());

        assertTrue(text.contains("Настройки WClock"));
        assertTrue(text.contains("Коллаж"));
        assertTrue(text.contains("Локация"));
        assertTrue(text.contains("Автоматически"));
        assertTrue(text.contains("Координаты"));
        assertFalse("Settings text must not contain mojibake fragments", text.contains("Р ") || text.contains("Рџ") || text.contains("СЃ"));
    }

    @Test
    public void automaticWeatherHidesApiKeyFields() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        View keyRow = findByTag(activity.getWindow().getDecorView(), SettingsActivity.TAG_WEATHER_KEYS_ROW);
        Spinner provider = findSpinnerWithFirstItem(activity.getWindow().getDecorView(), "Автоматически");

        assertNotNull(keyRow);
        assertNotNull(provider);
        assertEquals(View.GONE, keyRow.getVisibility());

        provider.setSelection(1);
        assertEquals(View.VISIBLE, keyRow.getVisibility());
        assertEquals(View.VISIBLE, ((ViewGroup) keyRow).getChildAt(0).getVisibility());
        assertEquals(View.GONE, ((ViewGroup) keyRow).getChildAt(1).getVisibility());

        provider.setSelection(2);
        assertEquals(View.GONE, ((ViewGroup) keyRow).getChildAt(0).getVisibility());
        assertEquals(View.VISIBLE, ((ViewGroup) keyRow).getChildAt(1).getVisibility());
    }

    @Test
    public void settingsDoNotFocusTextFieldOnOpen() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        View focused = activity.getWindow().getDecorView().findFocus();

        assertFalse("Opening settings should not focus an EditText and summon the soft keyboard", focused instanceof EditText);
    }

    private int countHorizontalRows(android.view.View view) {
        int count = view instanceof LinearLayout && ((LinearLayout) view).getOrientation() == LinearLayout.HORIZONTAL ? 1 : 0;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countHorizontalRows(group.getChildAt(i));
            }
        }
        return count;
    }

    private int countViews(android.view.View view, Class<?> viewClass) {
        int count = viewClass.isInstance(view) ? 1 : 0;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countViews(group.getChildAt(i), viewClass);
            }
        }
        return count;
    }

    private void assertRowHasColumns(SettingsActivity activity, String tag, int expectedColumns) {
        View row = findByTag(activity.getWindow().getDecorView(), tag);
        assertNotNull("Expected row tagged " + tag, row);
        assertTrue(row instanceof LinearLayout);
        assertEquals(LinearLayout.HORIZONTAL, ((LinearLayout) row).getOrientation());
        assertEquals(expectedColumns, ((LinearLayout) row).getChildCount());
    }

    private View findByTag(View view, String tag) {
        if (tag.equals(view.getTag())) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findByTag(group.getChildAt(i), tag);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Spinner findSpinnerWithFirstItem(View view, String firstItem) {
        if (view instanceof Spinner) {
            Spinner spinner = (Spinner) view;
            SpinnerAdapter adapter = spinner.getAdapter();
            if (adapter != null && adapter.getCount() > 0 && firstItem.equals(String.valueOf(adapter.getItem(0)))) {
                return spinner;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Spinner found = findSpinnerWithFirstItem(group.getChildAt(i), firstItem);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String collectText(View view) {
        StringBuilder builder = new StringBuilder();
        collectText(view, builder);
        return builder.toString();
    }

    private void collectText(View view, StringBuilder builder) {
        if (view instanceof TextView) {
            builder.append(((TextView) view).getText()).append('\n');
            builder.append(((TextView) view).getHint()).append('\n');
        }
        if (view instanceof Spinner) {
            SpinnerAdapter adapter = ((Spinner) view).getAdapter();
            if (adapter != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    builder.append(adapter.getItem(i)).append('\n');
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectText(group.getChildAt(i), builder);
            }
        }
    }
}
