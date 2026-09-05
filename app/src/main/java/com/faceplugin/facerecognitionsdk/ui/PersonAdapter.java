package com.faceplugin.facerecognitionsdk.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.faceplugin.facerecognitionsdk.R;
import com.faceplugin.facerecognitionsdk.kit.EnrolledPerson;
import com.faceplugin.facerecognitionsdk.kit.FaceRecognitionClient;

import java.util.ArrayList;
import java.util.Collections;

public class PersonAdapter extends ArrayAdapter<EnrolledPerson> {

    private final TextView txtEnrolledFace;

    public PersonAdapter(Context context, ArrayList<EnrolledPerson> personList, TextView textEnrolledFace) {
        super(context, 0, personList);
        txtEnrolledFace = textEnrolledFace;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        EnrolledPerson person = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_person, parent, false);
        }
        if (person == null) return convertView;

        TextView tvName = convertView.findViewById(R.id.textName);
        ImageView faceView = convertView.findViewById(R.id.imageFace);
        convertView.findViewById(R.id.buttonDelete).setOnClickListener(view -> {
            EnrolledPerson current = getItem(position);
            if (current == null) return;
            FaceRecognitionClient.get(getContext()).removeEnrolled(Collections.singleton(current.getId()));
            remove(current);
            notifyDataSetChanged();
            if (getCount() == 0) {
                txtEnrolledFace.setVisibility(View.INVISIBLE);
            } else {
                txtEnrolledFace.setVisibility(View.VISIBLE);
            }
        });

        tvName.setText(person.getName());
        Bitmap thumb = FaceRecognitionClient.get(getContext()).thumbnail(person);
        faceView.setImageBitmap(thumb);
        return convertView;
    }
}
