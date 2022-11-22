package com.vothilena.imageapp;

import static com.vothilena.imageapp.R.id.displayPicture;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.LinkAddress;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    protected static final String _FILE_NAME = "picture_list.txt";

    protected ImageView displayPicture;
    protected FloatingActionButton fabBack, fabNext, fabAdd;
    protected EditText pictureURL;
    protected Button btnTakePicture;
    //Arraylist variable to hold images
    protected ArrayList<String> _pictureList;
    //variable get the current position of the image
    protected int _currentPosition;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        pass in the id variables of the elements in the activity_main layout.
        displayPicture = (ImageView) findViewById(R.id.displayPicture);
        fabBack = findViewById(R.id.fab_back);
        fabNext = findViewById(R.id.fab_next);
        fabAdd = findViewById(R.id.fabAdd);
        pictureURL = findViewById(R.id.picture_url);
        btnTakePicture = findViewById(R.id.btnTakePicture);

        fabAdd.setOnClickListener(v -> insertPicture());
        //The next button calls the moveToNext() function.
        fabNext.setOnClickListener(v -> moveToNEXT());
        //The back button calls the Back To Previous() function.
        fabBack.setOnClickListener(v -> backToPrevious());
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });
        // call getPictureList() function
        _pictureList = getPictureList();
        //set initial value for current Position
        _currentPosition = 0;
        loadPicture();

    }

    private void insertPicture() {
        String pictureLink = pictureURL.getText().toString();
        if(isValidLink(pictureLink) == true){
            _pictureList.add(pictureLink);
            putLinkToFile(pictureLink);

            pictureURL.setText("");

            Toast.makeText(this, "Successfully inserted.", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "Link is not valid.", Toast.LENGTH_SHORT).show();

        }

    }

    private boolean isValidLink(String url){
        String standarLink = "((http|https)://)(www.)?"
                + "[a-zA-Z0-9@:%._\\+~#?&//=]"
                + "{2,256}\\.[a-z]"
                + "{2,6}\\b([-a-zA-Z0-9@:%"
                + "._\\+~#?&//=]*)";
        Pattern pattern = Pattern.compile(standarLink);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    private void moveToNEXT() {
        //Increase one unit to go to the next figure
        ++_currentPosition;
        //call loadPicture() to to get the image at this location
        loadPicture();
    }


    private void backToPrevious() {
        //Decrease one unit to return to the previous position
        --_currentPosition;
        //call loadPicture() to to get the image at this location
        loadPicture();
    }

    private ArrayList<String> getPictureList() {
        //array list to hold the images
        ArrayList<String> pictureList = new ArrayList<>();
        //add url to arraylist
        pictureList.add("https://th.bing.com/th/id/R.c599c5dae619c47b38d7e4bef4927d81?rik=q40Bo53T8XDm2g&riu=http%3a%2f%2f3.bp.blogspot.com%2f-TR6vyUJho2c%2fT0kBayeyYJI%2fAAAAAAAACf0%2fn3qWlbkp9bc%2fs1600%2ftree%2bpicture%2b4.jpg&ehk=uJcTSwtyR6NiRiOvHAqL0ycuucI893INjZY%2bUzNsuFU%3d&risl=&pid=ImgRaw&r=0");
        pictureList.add("https://th.bing.com/th/id/R.c355aa130ecc605abbf0c66f4a3e858b?rik=6s%2fsSwB1PrpnEA&riu=http%3a%2f%2fs0.geograph.org.uk%2fgeophotos%2f01%2f18%2f95%2f1189503_eca82fef.jpg&ehk=0S2OuNDkhTM43zrBeBAXsNW4bI20UH0vA2%2b7Mg6KQE0%3d&risl=&pid=ImgRaw&r=0");


        getPictureListFromFile(pictureList);

        Toast.makeText(this, "Successfully retrieved the List.", Toast.LENGTH_SHORT).show();

        return pictureList;
    }

    private void getPictureListFromFile(ArrayList<String> pictureList) {
        try {
            InputStream input = openFileInput(_FILE_NAME);

            if (input != null) {
                InputStreamReader inputReader = new InputStreamReader(input);
                BufferedReader bufferedReader = new BufferedReader(inputReader);

                String url = "";
                while ((url = bufferedReader.readLine()) != null) {
                    pictureList.add(url);
                }

                input.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "No files found.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void loadPicture() {
        //pass the image at the current location to the variable representing the Image View
        Picasso.with(this).load(_pictureList.get(_currentPosition)).into(displayPicture);
    }

    protected void putLinkToFile(String url) {
        try {
            OutputStreamWriter output = new OutputStreamWriter(openFileOutput(_FILE_NAME, MODE_APPEND));
            output.write(url);
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "No files found.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}