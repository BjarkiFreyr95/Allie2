package kos.allie;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.media.AudioFormat;
import android.media.AudioManager;
import javazoom.jl.decoder.*;
import javazoom.jl.converter.*;
import net.sourceforge.lame.mp3.*;
import net.sourceforge.lame.lowlevel.*;
import android.view.View;




import android.content.Context;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
//import javax.sound.sampled.AudioFormat;


public class MainActivity extends AppCompatActivity {
    AudioTrack audioTrack;
    String folderPath;
    List<String> filePaths;
    int currentSongPlayingIndex;
    int numberOfSongs;
    Thread thread;
    double audioStrength[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //creating the audiostrength array, 1100 because 44100 / 40 = ca 1100
        audioStrength = new double[1100];
        for (int i = 0; i < 1100; i++) {
            double temp = i;
            audioStrength[i] = Math.sin(((temp / 1100) * 2 * Math.PI)) * 0.5 + 0.5;
        }

        //printing the array
        for (int i = 0; i < 1100; i++) {
            if (i != 0) {
                Log.d("arraystuff", "" + audioStrength[i] + "  " + (audioStrength[1100 - i]));
            }
            else {
                Log.d("arraystuff", "" + audioStrength[i] + "  " + audioStrength[i]);
            }

        }


        folderPath = "";
        // checking if we have permission to storage.
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permisson", "Permission is granted");
        }
        else {
            Log.d("Permission", "asking for permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 9998);
        }
        Log.d("changing", "minBuffer");
        int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);



        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d("before", "playSound");
        //playSound();


    }

    @Override
    protected void onStart() {
        super.onStart();

        for (int i = 0; i < numberOfSongs; i++) {
            Log.d("FilePath", filePaths.get(i));
        }

        if (folderPath != "") {
            updateSelectedFolder();
        }

    }


    public void saveToTextFile(String toPrint, String fileName) {

        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AllieData";
        try
        {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File (fullPath, fileName);
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file, true));

            osw.write(toPrint);
            osw.flush();
            osw.close();
            Log.d("asdf","written to text file");
        }
        catch (Exception e)
        {
            Log.e("saveToExternalStorage()", e.getMessage());
        }
    }

    public void saveByteArrayAsFile(byte[] data) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AllieData/changedFilefhz.mp3");
        } catch (FileNotFoundException ex) {
            return;
        }
        try {
            fos.write(data);
        } catch (IOException ex) {
            return;
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (IOException ex) {
                return;
            }
        }
        Log.d("saveBytes", "succeeded");
    }



    public void buttonPlay(View v) {
        playSound();
    }



    private void playSound() {
        int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
        if (filePaths.get(currentSongPlayingIndex) != "") {
            final int count = 512 * 1024; // 512 kb
            //        byte[] byteData = null;
            //        byteData = new byte[(int)count];

            //we can decode correct byte data here

            final int duration = getMpthreeDuration();
            Log.d("duration", duration + "");




            audioTrack.play();
            thread = new Thread() {
                @Override
                public void run() {
                    byte[] byteData = null;
                    byte tempByte;
                    int asdf = 5;


                    try {
                        byteData = decode(filePaths.get(currentSongPlayingIndex), 0, duration);
                        Log.d("Bitedata sizeuuu", byteData.length + "");
                    }catch (IOException e) {
                        Log.d("IOException", "caught");
                    }

                    Log.d("before", "entering for loop");
                    short left = 0;
                    short right = 0;
                    int intTemp = 0;
                    int arrayIndexCounter = 0;
                    for (int i = 0; i < byteData.length - 4; i += 4) {

                        intTemp = ((byteData[i] + (byteData[i+1] << 8)) + (byteData[i+2] + (byteData[i+3] << 8)))/2 ;
                        left = (short) (intTemp * audioStrength[arrayIndexCounter]);
                        if (arrayIndexCounter != 0) {
                            right = (short) (intTemp * audioStrength[1100 - arrayIndexCounter]);
                        }
                        else {
                            right = (short) (intTemp * audioStrength[arrayIndexCounter]);
                        }
                        byteData[i] = (byte) (left &0xff);
                        byteData[i + 1] = (byte) ((left >> 8) &0xff);
                        byteData[i + 2] = (byte) (right &0xff);
                        byteData[i + 3] = (byte) ((right >> 8) &0xff);

                        arrayIndexCounter++;
                        if (arrayIndexCounter >= audioStrength.length) {
                            arrayIndexCounter = 0;
                        }
                    }
                    Log.d("out of", "for loop for changing byteData");


                    saveByteArrayAsFile(encodePcmToMp3(byteData));

                    int temp = 0;
                    while (temp < byteData.length)
                    {
                        audioTrack.write(byteData, temp, count);
                        Log.d("byteData", (Byte.toString(byteData[temp])) + "");

                        temp += count;
                    }
                    Log.d("stopping", "audiotrack");

                    audioTrack.stop();
                    audioTrack.release();
                }
            };
            thread.start();

        }
    }


    public int getMpthreeDuration() {
        int duration = 0;
        Header h= null;
        FileInputStream file = null;
        try {
            file = new FileInputStream(filePaths.get(currentSongPlayingIndex));
        } catch (Exception ex) {   }
        Bitstream bitstream = new  Bitstream(file);
        try {
            h = bitstream.readFrame();
        } catch (BitstreamException ex) {}
        try {
            duration = (int) h.total_ms((int) file.getChannel().size());
        }catch (IOException ex) {}
        if (duration == 0) {
            return 1000;
        }
        return duration;
    }

    public static byte[] decode(String path, int startMs, int maxMs)
            throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);

        float totalMs = 0;
        boolean seeking = true;

        File file = new File(path);
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file), 8 * 1024);
        try {
            Bitstream bitstream = new Bitstream(inputStream);
            Decoder decoder = new Decoder();

            boolean done = false;
            while (! done) {
                Header frameHeader = bitstream.readFrame();
                if (frameHeader == null) {
                    done = true;
                } else {
                    totalMs += frameHeader.ms_per_frame();

                    if (totalMs >= startMs) {
                        seeking = false;
                    }

                    if (! seeking) {
                        SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);

                        if (output.getSampleFrequency() != 44100
                                || output.getChannelCount() != 2) {
                            Log.d("mindtherobot", "mono or non-44100 MP3 not supported");
                        }

                        short[] pcm = output.getBuffer();
                        for (short s : pcm) {
                            outStream.write(s & 0xff);
                            outStream.write((s >> 8 ) & 0xff);
                        }
                    }

                    if (totalMs >= (startMs + maxMs)) {
                        done = true;
                    }
                }
                bitstream.closeFrame();
            }

            return outStream.toByteArray();
        } catch (BitstreamException e) {
            throw new IOException("Bitstream error: " + e);
        }
        catch (DecoderException e) {
            Log.w("asdf", "Decoder error", e);
        } finally {
            inputStream.close();
        }
        return null;
    }

    public void performFileSearch() {
        Log.d("inside", "performFileSearch");

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent.createChooser(intent, "Choose directory"), 9999);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case 9999:
                if (data != null) {
                    Uri uri = data.getData();
                    Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                            DocumentsContract.getTreeDocumentId(uri));
                    String path = getPath(this, docUri);
                    if (path != null) {
                        folderPath = path;
                    }
                    else {
                        folderPath = "";
                    }
                }
                break;
        }

    }

    public void updateSelectedFolder() {
        File folderFile = new File(folderPath);
        numberOfSongs = 0;
        filePaths = new ArrayList<String>();
        if (folderFile.isDirectory()) {
            File[] allFilesInFolder = folderFile.listFiles();

            if (allFilesInFolder != null) {
                currentSongPlayingIndex = 0;
                for (int i = 0; i < allFilesInFolder.length; i++) {
                    if (allFilesInFolder[i].getName().substring(allFilesInFolder[i].getName().length() - 4).toLowerCase().equals(".mp3")){
                        numberOfSongs++;
                        filePaths.add(folderPath + "/" + allFilesInFolder[i].getName());
                    }
                }
            }

        }

    }

    public byte[] encodePcmToMp3(byte[] pcm) {
        AudioFormat af = new AudioFormat.Builder().setSampleRate(44100).build();

        LameEncoder encoder = new LameEncoder(new javax.sound.sampled.AudioFormat(44100.0f, 16, 2, true, false),256, MPEGMode.STEREO, Lame.QUALITY_HIGHEST, false);



        ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
        byte[] buffer = new byte[encoder.getPCMBufferSize()];

        int bytesToTransfer = Math.min(buffer.length, pcm.length);
        int bytesWritten;
        int currentPcmPosition = 0;
        while (0 < (bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer))) {
            currentPcmPosition += bytesToTransfer;
            bytesToTransfer = Math.min(buffer.length, pcm.length - currentPcmPosition);

            mp3.write(buffer, 0, bytesWritten);
        }

        encoder.close();
        return mp3.toByteArray();
    }



    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                try {
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);
                } catch (NumberFormatException e) {
                    return null;
                }

            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == R.id.action_folder) {
            performFileSearch();
        }

        return super.onOptionsItemSelected(item);
    }
}
