package ca.cmpt276.project.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.SwitchCompat;

import ca.cmpt276.project.R;

public class SearchDialogFragment extends AppCompatDialogFragment {


    private SearchDialogListener dialogListener;
    private static final String TAG = "SearchDialogTag";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String NAME_FILTER_PREF = "name shared pref";
    private static final String HAZARD_FILTER_PREF = "hazard shared pref";
    private static final String NUM_FILTER_PREF = "num critical shared pref";
    private static final String LESS_FILTER_PREF = "less shared pref";
    private static final String FAV_FILTER_PREF = "favourite shared pref";
    EditText searchTxt;
    EditText criticalTxt;
    View v;
    private String searchTerm;
    private String hazardFilter;
    private int numCriticalFilter;
    private boolean favFilter;
    private String lessMore;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // code for alert dialog via fragment from Dr Brian Fraser's video https://www.youtube.com/watch?v=y6StJRn-Y-A
        v = LayoutInflater.from(getActivity()).inflate(R.layout.search_dialog_layout, null);
        getFiltersFromPref();
        setupTextFields();
        setupHazardRadioButtons();
        setupCriticalRadioButtons();
        setupFavFilter();
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case DialogInterface.BUTTON_POSITIVE:
                        String stringCritical = criticalTxt.getText().toString();
                        if(stringCritical.length() > 0) {
                            numCriticalFilter = Integer.parseInt(criticalTxt.getText().toString());
                        } else {
                            numCriticalFilter = -1;
                        }
                        searchTerm = searchTxt.getText().toString();
                        if(hazardFilter == null){
                            hazardFilter = getString(R.string.off);
                        }
                        if(lessMore == null){
                            lessMore = getString(R.string.off);
                        }
                        saveFiltersToPref(searchTerm, hazardFilter, numCriticalFilter, lessMore, favFilter);
                        dialogListener.sendSearchInput(searchTerm, hazardFilter, numCriticalFilter, lessMore, favFilter,false);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        Toast.makeText(getContext(), "Search cancelled", Toast.LENGTH_SHORT).show();
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        saveFiltersToPref("", getString(R.string.off), -1, getString(R.string.off), false);
                        dialogListener.sendSearchInput("one", hazardFilter, numCriticalFilter, lessMore, favFilter,true);
                        Toast.makeText(getContext(), "Reset search", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.search)
                .setView(v)
                .setPositiveButton(R.string.search, listener)
                .setNegativeButton(R.string.cancel, listener)
                .setNeutralButton(R.string.reset, listener)
                .create();
    }

    private void setupFavFilter() {
        CheckBox favouritesCheck = v.findViewById(R.id.checkBox_fav);
        favouritesCheck.setOnClickListener(v -> {
            favFilter = favouritesCheck.isChecked();
        });
        if(favFilter){
            favouritesCheck.setChecked(true);
        }
    }

    private void setupCriticalRadioButtons() {
        RadioGroup group = v.findViewById(R.id.radioGroupLessMore);
        String[] lessMoreSelections = getResources().getStringArray(R.array.less_more_selection);
        for (final String selectedLessMore : lessMoreSelections) {
            RadioButton btn = new RadioButton(getContext());
            btn.setText(selectedLessMore);
            btn.setOnClickListener(v -> {
                lessMore = selectedLessMore;
            });
            group.addView(btn);

            if (selectedLessMore.equals(lessMore)){
                btn.setChecked(true);
            }
        }

    }

    private void setupHazardRadioButtons() {
        RadioGroup group = v.findViewById(R.id.radioGroupHazard);
        String[] hazardSelectionsArray = getResources().getStringArray(R.array.hazard_selection);
        for (final String selectedHazardFilter : hazardSelectionsArray) {
            RadioButton btn = new RadioButton(getContext());
            btn.setText(selectedHazardFilter);
            btn.setOnClickListener(v -> {
                hazardFilter = selectedHazardFilter;
            });
            group.addView(btn);

            if(selectedHazardFilter.equals(hazardFilter)){
                btn.setChecked(true);
            }
        }

    }

    private void setupTextFields(){
        searchTxt = v.findViewById(R.id.inputSearchBar);
        criticalTxt = v.findViewById(R.id.inputCriticalFilter);
        searchTxt.setText(searchTerm);
        String numCriticalStr = "";
        if(numCriticalFilter >= 0) {
            numCriticalStr = Integer.toString(numCriticalFilter);
        }
        criticalTxt.setText(numCriticalStr);
    }

    private void getFiltersFromPref(){
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, 0);
        searchTerm = prefs.getString(NAME_FILTER_PREF, "");
        hazardFilter = prefs.getString(HAZARD_FILTER_PREF, this.getString(R.string.off));
        numCriticalFilter = prefs.getInt(NUM_FILTER_PREF, -1);
        lessMore = prefs.getString(LESS_FILTER_PREF, this.getString(R.string.off));
        favFilter = prefs.getBoolean(FAV_FILTER_PREF, false);
    }

    private void saveFiltersToPref(String name, String hazard, int numCritical, String lessMore, boolean fav){
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(NAME_FILTER_PREF, name);
        editor.putString(HAZARD_FILTER_PREF, hazard);
        editor.putInt(NUM_FILTER_PREF, numCritical);
        editor.putString(LESS_FILTER_PREF, lessMore);
        editor.putBoolean(FAV_FILTER_PREF, fav);
        editor.apply();
    }

    // code to send data from dialog to activity from https://www.youtube.com/watch?v=ARezg1D9Zd0
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            dialogListener = (SearchDialogListener) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "On attach: ClassCastException" + e.getMessage());
        }
    }

    public interface SearchDialogListener{
        void sendSearchInput(String input, String hazard_filter, int num_critical_filter, String lessMore, boolean favFilter,boolean reset);
    }

}
