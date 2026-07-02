package com.sstpnk.wclock.settings;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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

        assertTrue("Photo modes, provider, location, refresh and weather icon controls should use dropdown spinners", countViews(activity.getWindow().getDecorView(), Spinner.class) >= 6);
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
        assertTrue(text.contains("Локация для подписи"));
        assertTrue(text.contains("Количество фото на экране"));
        assertTrue(text.contains("Частота обновления погоды"));
        assertTrue(text.contains("Стиль погодных иконок"));
        assertTrue(text.contains("Автояркость использует"));
        assertFalse("Settings text must not contain mojibake fragments", text.contains("Р ") || text.contains("Рџ") || text.contains("СЃ"));
    }

    @Test
    public void photoModeAndOrderUseDropdowns() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();

        Spinner mode = findSpinnerWithFirstItem(activity.getWindow().getDecorView(), "Фотостена");
        Spinner order = findSpinnerWithFirstItem(activity.getWindow().getDecorView(), "Случайно");

        assertNotNull(mode);
        assertEquals("Фоторамка", mode.getAdapter().getItem(1));
        assertNotNull(order);
        assertEquals("По порядку", order.getAdapter().getItem(1));
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

    @Test
    public void dependentSettingsHideWhenTheirFeatureIsDisabled() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        View root = activity.getWindow().getDecorView();

        CheckBox collage = findCheckBox(root, "Включить коллаж");
        View photoGroup = findByTag(root, SettingsActivity.TAG_PHOTO_SETTINGS_GROUP);
        CheckBox clock = findCheckBox(root, "Показывать часы");
        View clockGroup = findByTag(root, SettingsActivity.TAG_CLOCK_SETTINGS_GROUP);
        CheckBox weather = findCheckBox(root, "Показывать погоду");
        View weatherGroup = findByTag(root, SettingsActivity.TAG_WEATHER_SETTINGS_GROUP);
        CheckBox autoBrightness = findCheckBox(root, "Автояркость по датчику");
        View autoRange = findByTag(root, SettingsActivity.TAG_BRIGHTNESS_AUTO_RANGE_ROW);

        assertNotNull(collage);
        assertNotNull(photoGroup);
        assertNotNull(clock);
        assertNotNull(clockGroup);
        assertNotNull(weather);
        assertNotNull(weatherGroup);
        assertNotNull(autoBrightness);
        assertNotNull(autoRange);

        collage.setChecked(false);
        assertEquals(View.GONE, photoGroup.getVisibility());

        clock.setChecked(false);
        assertEquals(View.GONE, clockGroup.getVisibility());

        weather.setChecked(false);
        assertEquals(View.GONE, weatherGroup.getVisibility());

        autoBrightness.setChecked(false);
        assertEquals(View.GONE, autoRange.getVisibility());
    }

    @Test
    public void panelAlphaControlsBelongToTheirFeatureGroups() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        View root = activity.getWindow().getDecorView();
        String text = collectText(root);

        assertTrue(text.contains("Прозрачность подложки часов"));
        assertTrue(text.contains("Прозрачность подложки погоды"));
        assertRowHasColumns(activity, SettingsActivity.TAG_CLOCK_SETTINGS_GROUP, 2);

        CheckBox clock = findCheckBox(root, "Показывать часы");
        CheckBox weather = findCheckBox(root, "Показывать погоду");
        View clockGroup = findByTag(root, SettingsActivity.TAG_CLOCK_SETTINGS_GROUP);
        View weatherGroup = findByTag(root, SettingsActivity.TAG_WEATHER_SETTINGS_GROUP);

        clock.setChecked(false);
        weather.setChecked(false);

        assertEquals(View.GONE, clockGroup.getVisibility());
        assertEquals(View.GONE, weatherGroup.getVisibility());
    }

    @Test
    public void weatherIconStyleUsesDropdown() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        Spinner spinner = findSpinnerWithFirstItem(activity.getWindow().getDecorView(), "Контурные");

        assertNotNull(spinner);
        assertEquals(2, spinner.getAdapter().getCount());
        assertEquals("Цветные", spinner.getAdapter().getItem(1));
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

    private CheckBox findCheckBox(View view, String text) {
        if (view instanceof CheckBox && text.equals(String.valueOf(((CheckBox) view).getText()))) {
            return (CheckBox) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                CheckBox found = findCheckBox(group.getChildAt(i), text);
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
