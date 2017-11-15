package com.jinosh.example;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jinosh.signatureview.SignaturePad;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SignaturePad signatureView;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private Button clear;
    private Button save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        signatureView = (SignaturePad) findViewById(R.id.signaturePad);
        if (!checkPermission())
            requestPermission();

        clear = (Button) findViewById(R.id.id_btn_clear);
        save = (Button) findViewById(R.id.id_btn_save);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signatureView.clearSignaturePad();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File directory = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File file = new File(directory, System.currentTimeMillis() + ".png");
                FileOutputStream out = null;
                Bitmap bitmap = signatureView.getSignatureBitmap();
                try {
                    out = new FileOutputStream(file);
                    if (bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } else {
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.flush();
                            out.close();

                            if (bitmap != null) {
                                Toast.makeText(getApplicationContext(),
                                        "Image saved successfully at " + file.getPath(), Toast.LENGTH_LONG).show();
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                                    new CustomMediaScanner(MainActivity.this, file);
                                } else {
                                    ArrayList<String> toBeScanned = new ArrayList<String>();
                                    toBeScanned.add(file.getAbsolutePath());
                                    String[] toBeScannedStr = new String[toBeScanned.size()];
                                    toBeScannedStr = toBeScanned.toArray(toBeScannedStr);
                                    MediaScannerConnection.scanFile(MainActivity.this, toBeScannedStr, null, null);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        Log.e(TAG,"onResume");
        super.onResume();
    }


    @Override
    protected void onStart() {
        Log.e(TAG,"onStart");
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Log.e(TAG,"onSaveInstanceState started");
        signatureView.getSignatureBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        outState.putByteArray("SIGNATURE_BITMAP_PROGRESS", byteArray);
        Log.e(TAG,"onSaveInstanceState finished");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.e(TAG,"onRestoreInstanceState started");
        super.onRestoreInstanceState(savedInstanceState);
        byte[] bitmapProgressArray = savedInstanceState.getByteArray("SIGNATURE_BITMAP_PROGRESS");
        Bitmap bitmapProgress = BitmapFactory.decodeByteArray(savedInstanceState.getByteArray("SIGNATURE_BITMAP_PROGRESS"), 0, bitmapProgressArray.length);
        signatureView.setLastSignBitmap(bitmapProgress);
        Log.e(TAG,"onRestoreInstanceState finished");
    }

    public class CustomMediaScanner implements
            MediaScannerConnection.MediaScannerConnectionClient {

        private MediaScannerConnection mSC;
        private File file;

        public CustomMediaScanner(Context context, File file) {
            this.file = file;
            mSC = new MediaScannerConnection(context, this);
            mSC.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            mSC.scanFile(file.getAbsolutePath(), null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            mSC.disconnect();
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);

        return result == PackageManager.PERMISSION_GRANTED ;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);

    }
}
