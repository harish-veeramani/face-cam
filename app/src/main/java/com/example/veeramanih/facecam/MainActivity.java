package com.example.veeramanih.facecam;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.firebase.FirebaseApp;
import com.otaliastudios.cameraview.*;
import husaynhakeem.io.facedetector.FaceBoundsOverlay;
import husaynhakeem.io.facedetector.FaceDetector;
import husaynhakeem.io.facedetector.models.Frame;
import husaynhakeem.io.facedetector.models.Size;
import okhttp3.*;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;

import static android.view.View.inflate;

public class MainActivity extends AppCompatActivity {
    private static String URL = "https://facejam.herokuapp.com/";
    private static String TOKEN = "CHp7Qbdb2rqr";
    private static String SLACK_AUTH_KEY = "xoxp-56919177042-367411298544-409784546038-41c52850ea78f2d3e6c21851b3564630";
    private static String SLACK_BASE_URL = "https://slack.com/api";

    private CameraView cameraView;
    private ConstraintLayout rootView;
    private ImageView capture;
    private FaceDetector faceDetector;
    private FaceBoundsOverlay faceBoundsOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_camera);
        rootView = findViewById(R.id.root_view);
        cameraView = findViewById(R.id.camera);
        capture = findViewById(R.id.capturePhoto);
        faceBoundsOverlay = findViewById(R.id.face_overlay);
        faceDetector = new FaceDetector(faceBoundsOverlay);
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER); // Tap to focus!
        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                Log.d("MainActivity", "picture taken");

                // Create a bitmap
                //Bitmap result = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                CameraUtils.decodeBitmap(picture, new CameraUtils.BitmapCallback() {
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        Log.d("MainActivity", "ready to send image");
                        //saveImage(bitmap, "Test");
                        sendImage(createFileFromBitmap(bitmap));
                    }
                });
            }
        });
        cameraView.addFrameProcessor(new FrameProcessor() {
            @UiThread
            @Override
            public void process(@NonNull com.otaliastudios.cameraview.Frame frame) {
                try {
                    Frame detectorFrame = new Frame(
                            frame.getData(),
                            frame.getRotation(),
                            new Size(frame.getSize().getWidth(), frame.getSize().getHeight()),
                            frame.getFormat(),
                            cameraView.getFacing().equals(Facing.BACK));
                    faceDetector.process(detectorFrame);
                } catch (Exception e) {
                    Log.d("MainActivity", "Null frame: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.captureSnapshot();
                Log.d("MainActivity", "tapped");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.destroy();
    }

    private void saveImage(Bitmap finalBitmap, String imageName) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + imageName + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendImage(File file) {

        try {
            final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");
            final MediaType MEDIA_TYPE_BMP = MediaType.parse("image/bmp");

            RequestBody req = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "face.jpg", RequestBody.create(MEDIA_TYPE_JPEG, file))
                    .addFormDataPart("token", TOKEN)
                    .build();

            Request request = new Request.Builder()
                    .url(URL)
                    .post(req)
                    .build();

            final Handler handler = new Handler(Looper.getMainLooper());
            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("MainActivity", "onFailure: " + e.getClass().getCanonicalName());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        JSONObject jsonObject = handleResponse(response);
                        if (jsonObject != null) {
                            final String slackId = jsonObject.get("SLACK_ID").toString();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    openPopup(slackId);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "onResponse error: " + e.getClass().getCanonicalName());
                        e.printStackTrace();
                    }
                }
            });
            //Response response = client.newCall(request).execute();
        } catch (Exception e) {
            Log.e("MainActivity", "Other Error: " + e.getClass().getCanonicalName());
            e.printStackTrace();
        }
    }

    private JSONObject handleResponse(Response response) {
        try {
            String jsonString = response.body().string();
            Log.d("MainActivity", jsonString);
            if (jsonString.length() == 2) return null;
            return new JSONObject(jsonString.substring(1, jsonString.length() - 1));
        } catch (Exception e) {
            Log.e("MainActivity", e.getClass().getCanonicalName());
        }
        return null;
    }

    private void openPopup(String slackId) {
        SlackUser user = createSlackUser(slackId);
        View view = inflate(this, R.layout.info_bottom_sheet, null);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);
        TextView nameTextView = view.findViewById(R.id.fragment_bottom_sheet_popup_name_text_view);
        if (user.getName() != null) {
            nameTextView.setText(user.getName());
        }
        TextView infoTextView = view.findViewById(R.id.fragment_bottom_sheet_popup_body_text_view);
        if (user.getTitle() != null) {
            infoTextView.setText(user.getTitle());
        }
        Button dismiss = view.findViewById(R.id.fragment_bottom_sheet_popup_button);
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        Log.d("MainActivity", "Opening popup...");
        dialog.show();
    }

    private SlackUser createSlackUser(String slackId) {
        final SlackUser user = new SlackUser();
        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = HttpUrl.parse(SLACK_BASE_URL + "/users.profile.get").newBuilder()
                .addQueryParameter("token", SLACK_AUTH_KEY)
                .addQueryParameter("user", slackId)
                .build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        Log.d("MainActivity", request.toString());

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MainActivity", "onFailure create slack user " + e.getClass().getCanonicalName());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonString = response.body().string();
                Log.d("MainActivity", jsonString);
                try {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    JSONObject profile = jsonObject.getJSONObject("profile");
                    String name = profile.getString("real_name");
                    String title = profile.getString("title");
                    user.setName(name);
                    user.setTitle(title);
                } catch (Exception e) {
                    Log.e("MainActivity", "onResponseError create slack user: " + e.getClass().getCanonicalName());
                    e.printStackTrace();
                }
            }
        });
        return user;
    }

    private File createFileFromBitmap(Bitmap bmp) {
        //create a file to write bitmap data
        File f = new File(new File(Environment.getExternalStorageDirectory().toString()), "face.jpg");
        try {
            f.createNewFile();
        } catch (IOException e) {
            Log.e("MainActivity", "Error: " + e.getLocalizedMessage());

        }

        //Convert bitmap to byte array
        Bitmap bitmap = bmp;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();

        //write the bytes in
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e("MainActivity", "Error: " + e.getLocalizedMessage());
        }

        return f;
    }

//    private static class ImageSenderTask extends AsyncTask<File, Void, JSONObject> {
//
//        @Override
//        protected JSONObject doInBackground(File... files) {
//            try {
//                final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");
//                final MediaType MEDIA_TYPE_BMP = MediaType.parse("image/bmp");
//
//                RequestBody req = new MultipartBody.Builder()
//                        .setType(MultipartBody.FORM)
//                        .addFormDataPart("file", "face.jpg", RequestBody.create(MEDIA_TYPE_JPEG, files[0]))
//                        .addFormDataPart("token", token)
//                        .build();
//
//                Request request = new Request.Builder()
//                        .url(url)
//                        .post(req)
//                        .build();
//
//                OkHttpClient client = new OkHttpClient();
//                Response response = client.newCall(request).execute();
//
//                //Log.d("MainActivity", "uploadImage:" + response.body().string());
//                String string = response.body().string();
//                JSONObject jsonObject = new JSONObject(string.substring(1, string.length() - 1));
//
//                return jsonObject;
//            } catch (UnknownHostException | UnsupportedEncodingException e) {
//                Log.e("MainActivity", "Error: " + e.getLocalizedMessage());
//            } catch (Exception e) {
//                Log.e("MainActivity", "Other Error: " + e.getClass().getCanonicalName());
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(JSONObject feed) {
//            try {
//                if (feed != null) {
//                    Log.d("MainActivity", "JSON:\n" + feed.toString(4));
//                } else {
//                    Log.d("MainActivity", "feed is NULL");
//                }
//            } catch (JSONException e) {
//                Log.e("MainActivity", "JSON exception");
//            }
//        }
//    }
}
