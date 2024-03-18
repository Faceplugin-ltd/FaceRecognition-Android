package com.faceplugin.facerecognition;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class PersonAdapter extends ArrayAdapter<Person> {

    DBManager dbManager;
    TextView txtEnrolledFace;
    public PersonAdapter(Context context, ArrayList<Person> personList, TextView textEnrolledFace) {
        super(context, 0, personList);

        dbManager = new DBManager(context);
        txtEnrolledFace = textEnrolledFace;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Person person = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_person, parent, false);
        }

        TextView tvName = (TextView) convertView.findViewById(R.id.textName);
        ImageView faceView = (ImageView) convertView.findViewById(R.id.imageFace);
        convertView.findViewById(R.id.buttonDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbManager.deletePerson(DBManager.personList.get(position).name);
                notifyDataSetChanged();
                if (DBManager.personList.size() == 0){
                    txtEnrolledFace.setVisibility(View.INVISIBLE);
                } else {
                    txtEnrolledFace.setVisibility(View.VISIBLE);
                }
            }
        });

        tvName.setText(person.name);
        faceView.setImageBitmap(person.face);
        // Return the completed view to render on screen
        return convertView;
    }
}