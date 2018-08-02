package com.example.veeramanih.facecam;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.firebase.FirebaseApp;
import com.otaliastudios.cameraview.*;
import husaynhakeem.io.facedetector.FaceBoundsOverlay;
import husaynhakeem.io.facedetector.FaceDetector;
import husaynhakeem.io.facedetector.models.Frame;
import husaynhakeem.io.facedetector.models.Size;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static android.view.View.inflate;

public class MainActivity extends AppCompatActivity {
    private static String URL = "http://192.168.145.27:6000";
    private static String TOKEN = "CHp7Qbdb2rqr";
    private static String SLACK_AUTH_KEY = "xoxp-56919177042-367411298544-409784546038-41c52850ea78f2d3e6c21851b3564630";
    private static String SLACK_BASE_URL = "https://slack.com/api";

    private CameraView cameraView;
    private ConstraintLayout rootView;
    private ImageView capture;
    private FaceDetector faceDetector;
    private FaceBoundsOverlay faceBoundsOverlay;
    private BottomSheetDialog dialog;
    private ProgressBar progressBar;
    private TextView nameTextView;
    private TextView titleTextView;
    private TextView emailTextView;
    private Button phoneButton;
    private Button vimeoButton;
    private Button twitterButton;
    private Button instagramButton;
    private Button githubButton;

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

        dialog = new BottomSheetDialog(this);
        final View view = inflate(this, R.layout.info_bottom_sheet, null);
        dialog.setContentView(view);

        nameTextView = view.findViewById(R.id.fragment_bottom_sheet_popup_name_text_view);
        progressBar = view.findViewById(R.id.loading);
        titleTextView = view.findViewById(R.id.fragment_bottom_sheet_popup_title_text_view);
        emailTextView = view.findViewById(R.id.fragment_bottom_sheet_popup_email_text_view);
        phoneButton = view.findViewById(R.id.phone);
        vimeoButton = view.findViewById(R.id.vimeo);
        twitterButton = view.findViewById(R.id.twitter);
        instagramButton = view.findViewById(R.id.instagram);
        githubButton = view.findViewById(R.id.github);


        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        cameraView.mapGesture(Gesture.LONG_TAP, GestureAction.FOCUS_WITH_MARKER);
        cameraView.mapGesture(Gesture.TAP, GestureAction.CAPTURE); // Tap to focus!
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
                        resetDialog();
                        dialog.show();
                        sendImage(createFileFromBitmap(bitmap), view);
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
                    //e.printStackTrace();
                }
            }
        });
    }

    private void resetDialog() {
        nameTextView.setVisibility(View.INVISIBLE);
        titleTextView.setVisibility(View.INVISIBLE);
        emailTextView.setVisibility(View.INVISIBLE);
        phoneButton.setVisibility(View.INVISIBLE);
        vimeoButton.setVisibility(View.INVISIBLE);
        twitterButton.setVisibility(View.INVISIBLE);
        instagramButton.setVisibility(View.INVISIBLE);
        githubButton.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
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

    private void sendImage(File file, final View view) {

        try {
            final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

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
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            titleTextView.setText("Request timed out...");
                            titleTextView.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                    Log.e("MainActivity", "onFailure: " + e.getClass().getCanonicalName());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        JSONObject jsonObject = handleResponse(response);
                        if (jsonObject != null) {
                            final String slackId = jsonObject.get("SLACK_ID").toString();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    final SlackUser user = new SlackUser();

                                    createSlackUser(slackId, user, new BottomSheetCallback() {
                                        @Override
                                        public void onPopulateUser() {
                                            Log.d("MainActivity", user.toString());

                                            progressBar.setVisibility(View.GONE);

                                            if (user.getName() != null) {
                                                nameTextView.setText(user.getName());
                                                nameTextView.setVisibility(View.VISIBLE);
                                            }

                                            titleTextView.setText(user.getTitle());
                                            titleTextView.setVisibility(View.VISIBLE);

                                            emailTextView.setText(user.getEmail());
                                            emailTextView.setVisibility(View.VISIBLE);

                                            if (user.getPhoneNumber() != null) {
                                                phoneButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent intent = new Intent(Intent.ACTION_DIAL);
                                                        intent.setData(Uri.parse("tel:" + user.getPhoneNumber()));
                                                        startActivity(intent);
                                                    }
                                                });
                                                phoneButton.setVisibility(View.VISIBLE);
                                            }

                                            if (user.getVimeo() != null) {
                                                vimeoButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                                        intent.setData(Uri.parse(user.getVimeo()));
                                                        startActivity(intent);
                                                    }
                                                });
                                                vimeoButton.setVisibility(View.VISIBLE);
                                            }

                                            if (user.getTwitter() != null) {
                                                twitterButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                                        intent.setData(Uri.parse(user.getTwitter()));
                                                        startActivity(intent);
                                                    }
                                                });
                                                twitterButton.setVisibility(View.VISIBLE);
                                            }

                                            if (user.getInstagram() != null) {
                                                instagramButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                                        intent.setData(Uri.parse(user.getInstagram()));
                                                        startActivity(intent);
                                                    }
                                                });
                                                instagramButton.setVisibility(View.VISIBLE);
                                            }

                                            if (user.getGithub() != null) {
                                                githubButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                                        intent.setData(Uri.parse(user.getGithub()));
                                                        startActivity(intent);
                                                    }
                                                });
                                                githubButton.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    });
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    titleTextView.setText("503 Bad Response");
                    titleTextView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            });

            Log.e("MainActivity", e.getClass().getCanonicalName());
        }
        return null;
    }

    private void createSlackUser(final String slackId, SlackUser user, final BottomSheetCallback callback) {
        final SlackUser slackUser = user;
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
                    slackUser.setName(name);
                    try {
                        String title = profile.getString("title");
                        slackUser.setTitle(title);
                    } catch (JSONException j) {
                        slackUser.setTitle(null);
                    }
                    try {
                        String email = profile.getString("email");
                        slackUser.setEmail(email);
                    } catch (JSONException j) {
                        slackUser.setEmail(null);
                    }
                    try {
                        String phone = profile.getString("phone");
                        slackUser.setPhoneNumber(phone);
                    } catch (JSONException j) {
                        slackUser.setPhoneNumber(null);
                    }
                    try {
                        String vimeoURL = profile.getJSONObject("fields").getJSONObject("Xf2BPE86JH").getString("value");
                        slackUser.setVimeo(vimeoURL);
                    } catch (JSONException j) {
                        slackUser.setVimeo(null);
                    }
                    try {
                        String twitterURL = profile.getJSONObject("fields").getJSONObject("Xf2C7ZGVM2").getString("value");
                        slackUser.setTwitter(twitterURL);
                    } catch (JSONException j) {
                        slackUser.setTwitter(null);
                    }
                    try {
                        String instagramURL = profile.getJSONObject("fields").getJSONObject("Xf2C82RLMC").getString("value");
                        slackUser.setInstagram(instagramURL);
                    } catch (JSONException j) {
                        slackUser.setInstagram(null);
                    }
                    try {
                        String githubURL = profile.getJSONObject("fields").getJSONObject("Xf2C8CU71V").getString("value");
                        slackUser.setGithub(githubURL);
                    } catch (JSONException j) {
                        slackUser.setGithub(null);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onPopulateUser();
                        }
                    });
                } catch (Exception e) {
                    Log.e("MainActivity", "onResponseError create slack user: " + e.getClass().getCanonicalName());
                    e.printStackTrace();
                }
            }
        });
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
}
