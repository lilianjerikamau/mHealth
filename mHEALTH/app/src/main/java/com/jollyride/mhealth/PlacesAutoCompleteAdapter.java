package com.jollyride.mhealth;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable {

    private final List<AutocompletePrediction> mResultList = new ArrayList<>();
    private final AutocompleteSessionToken token;
    private final PlacesClient placesClient;

    public PlacesAutoCompleteAdapter(@NonNull Context context) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        token = AutocompleteSessionToken.newInstance();
        placesClient = Places.createClient(context);
    }

    @Override
    public int getCount() {
        return mResultList.size();
    }

    @Override
    public AutocompletePrediction getItem(int position) {
        return mResultList.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                mResultList.clear();

                if (constraint != null) {
                    FindAutocompletePredictionsRequest request =
                            FindAutocompletePredictionsRequest.builder()
                                    .setSessionToken(token)
                                    .setQuery(constraint.toString())
                                    .build();

                    Task<FindAutocompletePredictionsResponse> task = placesClient.findAutocompletePredictions(request);
                    try {
                        Tasks.await(task, 1, TimeUnit.SECONDS);
                        if (task.isSuccessful() && task.getResult() != null) {
                            mResultList.addAll(task.getResult().getAutocompletePredictions());
                            results.values = mResultList;
                            results.count = mResultList.size();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof AutocompletePrediction) {
                    return ((AutocompletePrediction) resultValue).getFullText(null);
                }
                return super.convertResultToString(resultValue);
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);

        AutocompletePrediction item = getItem(position);
        if (item != null) {
            textView.setText(item.getFullText(null)); // show place name & address
        }

        return view;
    }

}
