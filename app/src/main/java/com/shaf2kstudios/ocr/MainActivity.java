package com.shaf2kstudios.ocr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Date;

public class MainActivity extends Activity  implements LocationListener {

	public static final String DATA_PATH = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/ocr/";
	private static final String TAG = "OCR";

	protected ImageButton _cameraButton;
	protected TextView _field;
	protected String _path = DATA_PATH + "ocr.jpg";
	protected File file;
	protected boolean _taken;
	protected boolean _cropped;
	protected Dialog load;

	private final int CAMERA_PICTURE = 1;
	private final int GALLERY_PICTURE = 2;

	protected static final String PHOTO_TAKEN = "photo_taken";
	protected static final String PHOTO_CROPPED = "photo_cropped";

	private LocationManager manager;
	
	boolean isGPSEnabled = false;
	boolean isNetworkEnabled = false;

	boolean canGetLocation = false;

	Location location; // location
	double latitude; // latitude
	double longitude; // longitude
	private String address;

	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
	private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

		manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

		//_image = (ImageView) findViewById(R.id.preview);
		_field = (TextView) findViewById(R.id.field);
		_cameraButton = (ImageButton) findViewById(R.id.camera);
		_cameraButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				_field.setText("");
				if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
					buildAlertMessageNoGps();
				} else {
					AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(MainActivity.this);
					myAlertDialog.setTitle("Select Picture Option");
					myAlertDialog.setMessage("How do you want to select your picture?");

					myAlertDialog.setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
							startActivityForResult(pictureActionIntent, GALLERY_PICTURE);
						}
					});

					myAlertDialog.setNegativeButton("Camera", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							Log.v(TAG, "Starting Camera app");
							startCameraActivity();
						}
					});
					myAlertDialog.show();
				}
			}
		});
		
		// Make the required directory
		File dir = new File(DATA_PATH);
		if(!dir.exists()) {
			dir.mkdir();
		}
		// create link to file ocr.jpg
		file = new File(_path);
	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("MTN Image Reader needs access to your location. Please turn on location access.")
		.setTitle("Location Services Disabled")
		.setCancelable(false)
		.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
				startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
				dialog.cancel();
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	protected void startCameraActivity() {
		
		Uri outputFileUri = Uri.fromFile(file);

		// use default android camera
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		startActivityForResult(intent, CAMERA_PICTURE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.i(TAG, "resultCode: " + resultCode);

		if (requestCode == CAMERA_PICTURE && resultCode == Activity.RESULT_OK ) {
			Log.v(TAG, "Photo Taken");
			onPhotoTaken(); // skip cropping
			onPhotoCropped();
		} else if( requestCode == GALLERY_PICTURE && resultCode == Activity.RESULT_OK ){
			Log.v(TAG, "Photo Selected from Gallery");

			Uri imageURI = data.getData();

			Log.v(TAG,"selected pic is "+imageURI);
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(imageURI, filePathColumn, null, null, null);
			cursor.moveToFirst();
			int column_index = cursor.getColumnIndex(filePathColumn[0]);
			String filePath = cursor.getString(column_index);
			Log.v(TAG,"selected pic is "+filePath);

			Bitmap selectedImage = BitmapFactory.decodeFile(filePath);
			FileOutputStream out;
			try {
				out = new FileOutputStream(file);
				selectedImage.compress(Bitmap.CompressFormat.JPEG, 50, out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			/*
            FileOutputStream out;
            Bitmap selectedImage;
            try {
                InputStream input = getContentResolver().openInputStream(imageURI);
                Log.v(TAG, "input stream = "+input);
                //Bitmap selectedImage = BitmapFactory.decodeFile(filePath);
                selectedImage = BitmapFactory.decodeStream(input);
                Log.v(TAG, "bitmap = "+selectedImage);
                out = new FileOutputStream(_path);
                selectedImage.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
			 */
			//onPhotoTaken(); // skip cropping
			onPhotoCropped();
		} else if ( resultCode == -1 ) {
			//onPhotoCropped();
		} else {
			Log.v(TAG, "User cancelled");
		}
	}

	public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
		int width = image.getWidth();
		int height = image.getHeight();

		float bitmapRatio = (float)width / (float) height;
		if (bitmapRatio > 0) {
			width = maxSize;
			height = (int) (width / bitmapRatio);
		} else {
			height = maxSize;
			width = (int) (height * bitmapRatio);
		}
		return Bitmap.createScaledBitmap(image, width, height, true);
	}

	protected void onPhotoTaken() {
		_taken = true;
		Log.v(TAG, "Launching CropImage Activity");
		Uri outputFileUri = Uri.fromFile(file);
		Log.v(TAG, "Pass uri = "+outputFileUri);
		final CropImageIntentBuilder builder = new CropImageIntentBuilder(100, 100, outputFileUri);

		Intent intent = builder.getIntent(this);
		startActivityForResult(intent,0);
	}

	protected void onPhotoCropped() {
		_cropped = true;

		Bitmap bitmap = getResizedBitmap(BitmapFactory.decodeFile(_path), 400);
		//_image.setImageBitmap(bitmap);
		FileOutputStream out;
		try {
			out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Log.v(TAG,"path is = "+file.getAbsolutePath());

		System.out.println("Connecting to socket to get captch result");
		new decodePhotoTask().execute();
		// Cycle done.
	}

	public void getLocation() {
		try {

			// getting GPS status
			isGPSEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
			isNetworkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (isGPSEnabled || isNetworkEnabled) {
				this.canGetLocation = true;
				if (isNetworkEnabled) {
					manager.requestLocationUpdates(
							LocationManager.NETWORK_PROVIDER,
							MIN_TIME_BW_UPDATES,
							MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
					Log.d("Network", "Network");
					if (manager != null) {
						location = manager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						if (location != null) {
							latitude = location.getLatitude();
							longitude = location.getLongitude();
						}
					}
				}
				// if GPS Enabled get lat/long using GPS Services
				if (isGPSEnabled) {
					if (location == null) {
						manager.requestLocationUpdates(
								LocationManager.GPS_PROVIDER,
								MIN_TIME_BW_UPDATES,
								MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
						Log.d("GPS Enabled", "GPS Enabled");
						if (manager != null) {
							location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							if (location != null) {
								latitude = location.getLatitude();
								longitude = location.getLongitude();
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(MainActivity.PHOTO_TAKEN, _taken);
		outState.putBoolean(MainActivity.PHOTO_CROPPED, _cropped);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		//Log.i(TAG, "onRestoreInstanceState()");
		if (savedInstanceState.getBoolean(MainActivity.PHOTO_CROPPED)) {
			onPhotoCropped();
		} else if (savedInstanceState.getBoolean(MainActivity.PHOTO_TAKEN)) {
			onPhotoTaken();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		return id == R.id.action_settings || super.onOptionsItemSelected(item);
	}

	@Override
	public void onLocationChanged(Location location) {

	}

	@Override
	public void onStatusChanged(String s, int i, Bundle bundle) {

	}

	@Override
	public void onProviderEnabled(String s) {

	}

	@Override
	public void onProviderDisabled(String s) {

	}

	private class decodePhotoTask extends AsyncTask<String, String, String> {
		private  Dialog dialog;
		
		@Override
	    protected void onPreExecute() {
			load = new Dialog(MainActivity.this);
			load.requestWindowFeature(Window.FEATURE_NO_TITLE);
			load.setContentView(R.layout.loading_dialog);
			load.setCancelable(false); 
			load.show();
			getLocation();
			
		}
		
		@Override
		protected String doInBackground(String... params) {
			String result = null;
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				FileInputStream inFile = new FileInputStream(file);

				int bucket;
				while ((bucket = inFile.read()) != -1) {
					out.write(bucket);
				}
				System.out.println("Read image with filesize: " + out.size());

				// connect to socket
				Socket socket;
				socket = new Socket("173.203.81.147", 3003);


				// get the output stream of the socket
				OutputStream ss = socket.getOutputStream();
				ss.write(out.toByteArray(), 0, out.toByteArray().length);
				ss.write("\r\n".getBytes());
				ss.flush();

				Thread.sleep(5000); // wait 5 seconds

				// get the result from the socket */
				out.reset();
				byte[] buf = new byte[1024];
				socket.getInputStream().read(buf, 0, 1024);

				out.write(buf);
				out.flush();

				System.out.println("Total bytes read: " + out.size());
				result = new String(out.toByteArray());
				System.out.println("Captcha results: " + result);

				// close all streams and socket
				ss.close();
				inFile.close();
				socket.close();
				
				// get location
				address = MapAPI.getAddress(location);

			} catch (ConnectException e) {
				return null;
			} catch (Exception e){
				e.printStackTrace();
			}

			return result;
		}

		@Override
		protected void onPostExecute(final String result){
			address = MapAPI.getAddress(location);
			
			if(load != null && load.isShowing()) load.dismiss();
			
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (result != null) {
						dialog = new Dialog(MainActivity.this);
						dialog.setContentView(R.layout.result_dialog);
						dialog.setTitle(R.string.ocr_output);


						TextView mr = (TextView) dialog.findViewById(R.id.textView1);
						mr.setText(mr.getText()+" "+result);
						TextView gps = (TextView) dialog.findViewById(R.id.textView2);
						if(location != null)
							gps.setText(gps.getText()+" "+location.getLatitude() + ", " + location.getLongitude());
						
						TextView loc = (TextView) dialog.findViewById(R.id.textView3);
						
						loc.setText(loc.getText()+" "+address);
						TextView date = (TextView) dialog.findViewById(R.id.textView4);
						date.setText(date.getText()+" "+new Timestamp(new Date().getTime()));

						Button button = (Button) dialog.findViewById(R.id.buttonSend);

						button.setOnClickListener(new android.view.View.OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
							}
						});
						dialog.show();

					} else {
						Toast.makeText(getApplicationContext(),"Could not connect to OCR Server",Toast.LENGTH_SHORT);
					}
				}
			});
		}
	}
}
