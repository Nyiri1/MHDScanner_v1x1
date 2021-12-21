package aaa.mhdscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    Button btnWidthConfirmButton, btnHeightConfirmButton;
    EditText etWidth, etHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v("1115", "onCreateMainMeu");

        btnWidthConfirmButton = (Button) findViewById(R.id.buttonConfirmWidth);
        btnHeightConfirmButton = (Button) findViewById(R.id.buttonConfirmHeight);
        etWidth = (EditText) findViewById(R.id.editTextWidth);
        etHeight = (EditText) findViewById(R.id.editTextHeight);

        //Grab lastdeswidth from settings
        //set text of etWidth with lastdesWidth
        etWidth.setText(Integer.toString(getLastDesiredWidth(this)));
        //Grab lastdesheight from settings
        //set text of etHeight with lastdesHeight
        etHeight.setText(Integer.toString(getLastDesiredHeight(this)));


        btnWidthConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //safe new value if valid
                int lastDesWidth = Integer.parseInt(etWidth.getText().toString());
                if( lastDesWidth <= getLastWidth(MainActivity.this) & lastDesWidth!=0){
                    etWidth.setText(Integer.toString(lastDesWidth));
                    setLastDesiredWidth(lastDesWidth);
                } else{
                    etWidth.setText(Integer.toString(getLastDesiredWidth(MainActivity.this)));
                }
                //set text of etWidth
            }
        });

        btnHeightConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //safe new value if valid
                int lastDesHeight = Integer.parseInt(etHeight.getText().toString());
                if( lastDesHeight <= getLastHeight(MainActivity.this) & lastDesHeight!=0){
                    etHeight.setText(Integer.toString(lastDesHeight));
                    setLastDesiredHeight(lastDesHeight);
                } else{
                    etHeight.setText(Integer.toString(getLastDesiredHeight(MainActivity.this)));
                }
                //set text of etHeight
            }
        });

        findViewById(R.id.button_open_camera1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCameraPreview();
            }
        });
    }

    public void showCameraPreview(){
        Intent intent = new Intent(this, Activity_MHDScan.class);
        startActivity(intent);
    }

    private void setLastDesiredWidth(Integer value){
        SharedPreferences prefs = this.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("LastDesWidth", value);
        editor.apply();
    }

    private void setLastDesiredHeight(Integer value){
        SharedPreferences prefs = this.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("LastDesHeight", value);
        editor.apply();
    }

    static public int getLastDesiredWidth(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastDesWidth", 100);
    }

    static public int getLastDesiredHeight(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastDesHeight", 100);
    }

    static public int getLastWidth(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastWidth", 0);
    }

    static public int getLastHeight(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastHeight", 0);
    }
}

