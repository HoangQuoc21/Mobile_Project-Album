package com.example.album;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import android.app.Dialog;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ImageActivity extends AppCompatActivity {
    View frame;
    private static final int REQUEST_WRITE_STORAGE = 112;

    // Button Back ở header
    ImageButton btnBack;

    // Button ở footer của image bình thường
    ImageButton btnAddAlbum,btnAddFavorite, btnDelete,btnEdit, btnInfo,btnDeleteInAlbum;

    // Button ở footer của image trong Trash
    ImageButton btnDeleteTrash, btnRestore;

    //Khai báo ImageView
    ImageView imageView;
    // Dùng để lưu ảnh mới sau khi chỉnh sửa từ ảnh gốc trong hoạt động này
    imageModel editedImage;

    //Khai báo ScaleGestureDetector dùng để scale ảnh (Zoom in, Zoom out)
    private ScaleGestureDetector scaleGestureDetector;

    // Khai báo giá trị Factor (giá trị scale)
    private float Factor = 1.0f;

    //Khai báo tọa độ lastX, last Y dùng để di chuyển ảnh
    private float lastX = 0.0f;
    private float lastY = 0.0f;

    ListView listViewAlbum;
    Uri imageUri;
    String nameAlbumToAdd="";
    int imageId;
    String imagePath;
    ArrayList<String>listNameAlbum= new ArrayList<>();

    // Nhận Dữ liệu danh sách Album từ activity
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if("listAlbumSender".equals(intent.getAction()))
            {
                listNameAlbum=intent.getStringArrayListExtra("listAlbum");
            }

            // lắng nghe broadcast tự động xóa sau 24h. Nếu ảnh người dùng đang xem
            // là ảnh đang được tự động xóa thì tự thoát khỏi ImageActivity
            if ("autoDeleteTrash".equals(intent.getAction()))
            {
                int imageIdDelete = intent.getIntExtra("imageId", -1);
                if(imageIdDelete == imageId)
                {
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        frame = findViewById(R.id.frame);

        //Lấy dữ liệu từ Main Activity
        Intent myIntent = getIntent();

        //Lấy bundle ra khỏi intent
        Bundle myBundle = myIntent.getBundleExtra("package");
        // id ảnh
        imageId = myBundle.getInt("imageId");
        // link ảnh
        imagePath = myBundle.getString("imageLink");
        // dateTaken của ảnh
        String imageDate = myBundle.getString("imageDate");
        // vị trí của ảnh trong 1 date (khi là ảnh bình thường)
        // hoặc vị trí của ảnh trong imageListTrash (khi là ảnh trong Trash)
        String imageIndex = myBundle.getString("imageIndex");
        // dùng để dán layout footer đúng với với ảnh bình thường hoặc ảnh trong Trash
        String footerLayout = myBundle.getString("footer");

        // khi là ảnh bình thường
        if(footerLayout.equals("1"))
        {
            // load layout footer của ảnh bình thường
            loadLayout(R.layout.image_all);

            //Nhận dữ liệu List Name Album
            Intent intentAddImageToAlbum= new Intent("addImageToAlbum");
            intentAddImageToAlbum.putExtra("Temp","");
            sendBroadcast(intentAddImageToAlbum);

            // Kết nối các nút Button với layout
            btnAddAlbum = (ImageButton) findViewById(R.id.btnAddAlbum);
            btnAddFavorite = (ImageButton) findViewById(R.id.btnAddFavorite);
            btnDelete = (ImageButton) findViewById(R.id.btnDelete);
            btnEdit = (ImageButton) findViewById(R.id.btnEdit);
            btnInfo = (ImageButton) findViewById(R.id.btnInfo);
            btnDeleteInAlbum=(ImageButton) findViewById(R.id.btnDeleteInAlbum);

            // Kiểm tra xem có ẩn hay hiện nút delete hay không?.
            if (ButtonStatusManager.getInstance().isButtonDisabled()) {
                btnDelete.setVisibility(View.GONE);
                btnDeleteInAlbum.setVisibility(View.VISIBLE);
            }
            else
            {
                btnDelete.setVisibility(View.VISIBLE);
                btnDeleteInAlbum.setVisibility(View.GONE);
            }


            // -----------------Xử lý sự kiện click-------------
            //Nút thêm Album
            btnAddAlbum.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //In ra thong bao de kiem tra
                    //Toast.makeText(ImageActivity.this, "Add", Toast.LENGTH_SHORT).show();

                    //Mo hop thoai add vao album
                    openDialogAddAlbum(Gravity.CENTER);
                }
            });

            //Nút thêm vào Album yêu thích
            btnAddFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Your code here.
                    // Toast.makeText(ImageActivity.this, "Add favorite", Toast.LENGTH_SHORT).show();
                    // gửi index của ảnh trong danh sách cho MainActivity bằng broadcast

                    Intent intentAddFavorite= new Intent("addFavorite");
                    intentAddFavorite.putExtra("imageLink",imagePath);
                    sendBroadcast(intentAddFavorite);
                }
            });

            //Nút Delete
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openDialogDelete(imageDate,imageIndex,imagePath);
                }
            });

            // Xử lý nút edit
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Toast.makeText(ImageActivity.this, "Edit", Toast.LENGTH_SHORT).show();
                    // Tạo intent, truyền imagePath và start activity image cropper với mong muốn
                    // nhận được kết quả là một imagePath của ảnh sau khi thực hiện chỉnh sửa
                    Intent editIntent = new Intent(ImageActivity.this, ImageCropper.class);
                    editIntent.putExtra("SendImageData", imagePath);
                    startActivityForResult(editIntent, 100);
                }
            });

            //Nút Xem thông tin
            btnInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //Phai them bien context thi moi doc thong tin exif cua anh trong bien imageModel duoc
                    Context context = ImageActivity.this;
                    //tao bien readImage de doc cac thong tin exif
                    imageModel readImage = new imageModel(imageUri, imagePath,context);

                    //tạo string chứa các thông tin exif
                    // Create a StringBuilder to format the exif information
                    StringBuilder sb = new StringBuilder();
                    sb.append("Name: ").append(readImage.name).append("\n");
                    sb.append("Saved place: ").append(readImage.savedPlace).append("\n");
                    sb.append("Date and time: ").append(imageDate).append("\n");
                    sb.append("Image length: ").append(readImage.imageLength).append(" pixels\n");
                    sb.append("Image width: ").append(readImage.imageWidth).append(" pixels\n");
                    sb.append("Camera make: ").append(readImage.cameraMake).append("\n");
                    sb.append("Camera model: ").append(readImage.cameraModel).append("\n");

                    //tạo hộp thoại dialog để hiển thị thông tin exif
                    // Create an AlertDialog.Builder object to build the dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(ImageActivity.this);
                    builder.setTitle("Image Information");
                    builder.setMessage(sb.toString());
                    builder.setPositiveButton("OK", null);

                    //hiển thị hộp thoại dialog
                    // Create and show the dialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
            // Nút yêu cầu xóa ảnh trong album
            btnDeleteInAlbum.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intentdeleteInAlbum= new Intent("deleteInAlbum");
                    String nameAlbum= ButtonStatusManager.getInstance().getNameAlbum();
                    intentdeleteInAlbum.putExtra("nameAlbum",nameAlbum);
                    intentdeleteInAlbum.putExtra("imageLink",imagePath);
                    sendBroadcast(intentdeleteInAlbum);
                    Toast.makeText(ImageActivity.this, "Successfully deleted image in album", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }

        // khi là ảnh trong Trash
        else
        {
            // load layout footer của ảnh trong Trash
            loadLayout(R.layout.image_trash);

            // Kết nối các nút Button với layout
            btnRestore = (ImageButton) findViewById(R.id.btnRestore);
            btnDeleteTrash = (ImageButton) findViewById(R.id.btnDelete);

            // xử lý sự kiện nhấn nút Restore 1 ảnh trong Trash
            btnRestore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(ImageActivity.this,"Image restored",Toast.LENGTH_LONG);

                    Intent intentRestore = new Intent("restoreImage");
                    intentRestore.putExtra("imageIndexTrash", imageIndex);
                    // add by Quan
                    intentRestore.putExtra("imageLink",imagePath);
                    // xong
                    sendBroadcast(intentRestore);
                    finish();
                }
            });

            // xử lý sự kiện nhấn nút Delete 1 ảnh trong Trash
            btnDeleteTrash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openDialogDeleteTrash(imageIndex);
                }
            });
        }


        //Nút Back để trở về Activity chính.
        btnBack = (ImageButton) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        try {
            // parse String path to Uri to create bitmap
            imageUri = Uri.parse(imagePath);
            Bitmap bm = BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(imageUri));
            if (bm != null) {
                //Kết nối với layout
                imageView = findViewById(R.id.imageView);

                // Đặt ảnh vào ImageView
                imageView.setImageBitmap(bm);

                //Xử lý scale ảnh (Zoom in, Zoom out);
                scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
            }
            else {
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Broadcast của click delete Album
        IntentFilter filter = new IntentFilter("listAlbumSender");
        // Broadcast của auto delete trash sau 24h
        IntentFilter filter_autoDeleteTrash = new IntentFilter("autoDeleteTrash");

        registerReceiver(receiver, filter);
        registerReceiver(receiver, filter_autoDeleteTrash);
    }


    //Xử lý event chạm vào ảnh (Move)
    private boolean isZooming = true;
    private float initialX1, initialY1, initialX2, initialY2;
    private float centerX, centerY;
    private float scaleFactor = 1.0f;
    private int pointerPrevious =1 ;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean check = scaleGestureDetector.onTouchEvent(event);

        int pointerCount = event.getPointerCount();

        if(pointerCount>1)
        {
            pointerPrevious = pointerCount;
        }
        if(pointerCount==1)
        {
            if(MotionEvent.ACTION_UP== event.getAction())
            {
                pointerPrevious=1;
            }
        }
        if (!isZooming) {
            if (pointerCount == 1 && pointerPrevious==1) {
                // Nếu chỉ có một ngón tay, xử lý sự kiện di chuyển
                    handleSingleTouchMove(event);
            }

        }

        return super.onTouchEvent(event);
    }

    private void handleSingleTouchMove(MotionEvent event) {
        float currentX = event.getX();
        float currentY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Lưu lại tọa độ khi bắt đầu di chuyển
                initialX1 = currentX;
                initialY1 = currentY;
                break;

            case MotionEvent.ACTION_MOVE:
                // Tính toán sự chênh lệch giữa tọa độ hiện tại và tọa độ khi bắt đầu di chuyển
                float deltaX = currentX - initialX1;
                float deltaY = currentY - initialY1;

                // Di chuyển trung tâm zoom (không thay đổi tổng dịch chuyển)
                centerX -= deltaX;
                centerY -= deltaY;

                // Áp dụng di chuyển cho ImageView
                imageView.setTranslationX(-centerX);
                imageView.setTranslationY(-centerY);

                // Cập nhật tọa độ khi bắt đầu di chuyển để sử dụng trong lần di chuyển tiếp theo
                initialX1 = currentX;
                initialY1 = currentY;

                break;
        }
    }

    class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isZooming = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isZooming = false;
            // Lưu lại tỷ lệ zoom cuối cùng
            scaleFactor = detector.getScaleFactor();
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (isZooming) {
                // Lấy độ zoom
                float scaleFactor = detector.getScaleFactor();
                Factor *= scaleFactor;
                Factor = Math.max(0.1f, Math.min(Factor, 10.f));

                // Cập nhật tỷ lệ zoom của ImageView
                imageView.setScaleX(Factor);
                imageView.setScaleY(Factor);
            }
            return true;
        }
    }

    // load layout footer tương ứng với 2 trường hợp: ảnh bình thường và ảnh trong Trash
    private void loadLayout(int layoutResId)
    {
        LayoutInflater inflater = LayoutInflater.from(this);
        View footerContent = inflater.inflate(layoutResId, (ViewGroup) frame, false);
        ((ViewGroup) frame).addView(footerContent);
    }

    // Xử lý dialog để add album
    private void openDialogAddAlbum(int gravity)
    {
        final Dialog dialog= new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_image_to_album);

        Window window= dialog.getWindow();
        if(window==null)
        {
            return;
        }
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams windowAttributes= window.getAttributes();
        windowAttributes.gravity=gravity;
        window.setAttributes(windowAttributes);

        if(Gravity.CENTER== gravity)
        {
            dialog.setCancelable(true);
        }
        else
        {
            dialog.setCancelable(false);
        }

        // Hiện listView

        listViewAlbum =(ListView)dialog.findViewById(R.id.lvAddImageToAlbum);
        Button btnDialogCheckAddImageBack=dialog.findViewById(R.id.btnDialogCheckAddImageBack);
        Button btnDialogCheckAddImageConfirm=dialog.findViewById(R.id.btnDialogCheckAddImageConfirm);
        TextView txtNameAlbumToAdd= dialog.findViewById(R.id.txtNameAlbumToAdd);
        ArrayList<Album> listAlbum= new ArrayList<>();


        for (int i=0;i<listNameAlbum.size();i++)
        {
            listAlbum.add(new Album(listNameAlbum.get(i)));
        }

        AlbumAdapter albumAdapter= new AlbumAdapter(ImageActivity.this,R.layout.list_albums,listAlbum);
        listViewAlbum.setAdapter(albumAdapter);
        // Xử lý khi click vào 1 album
        listViewAlbum.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                txtNameAlbumToAdd.setText(listNameAlbum.get(position));
                nameAlbumToAdd=listNameAlbum.get(position);
            }
        });
        // Xử lý click Back.
        btnDialogCheckAddImageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Xử lý click Back.
        btnDialogCheckAddImageConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nameAlbumToAdd.equals(""))
                {
                    Toast.makeText(ImageActivity.this, "Choose An Album To Add", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    // gửi index của ảnh trong danh sách cho MainActivity bằng broadcast

                    Intent addLinkImageToAlbumHadChoosen= new Intent("addLinkImageToAlbumHadChoosen");
                    addLinkImageToAlbumHadChoosen.putExtra("imageLink",imagePath);
                    addLinkImageToAlbumHadChoosen.putExtra("albumName",nameAlbumToAdd);
                    sendBroadcast(addLinkImageToAlbumHadChoosen);
                    Toast.makeText(ImageActivity.this, "Successfully added this image to selected album", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }
        });

        // gọi lệnh Show để hiện Dialog
        dialog.show();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // hủy đăng ký broadcast receiver
        unregisterReceiver(receiver);
    }

     // Xử lý hiện dialog xác nhận chuyển vào Thùng rác
    private void openDialogDelete(String imageDate, String imageIndex, String imagePath)
    {
        final Dialog dialog= new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_move_to_trash);

        Window window= dialog.getWindow();
        if(window==null)
        {
            return;
        }

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams windowAttributes= window.getAttributes();
        windowAttributes.gravity= Gravity.BOTTOM;
        window.setAttributes(windowAttributes);
        dialog.setCancelable(true);

        Button btnDialogCancel= dialog.findViewById(R.id.btnDialogCancel);
        Button btnDialogMove= dialog.findViewById(R.id.btnDialogMove);

        // Khi nhấn nút Cancel của Dialog
        btnDialogCancel.setOnClickListener(new View.OnClickListener() {
             @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Khi nhấn nút Move của Dialog
        btnDialogMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intentDelete = new Intent("deleteImage");
                intentDelete.putExtra("imageIndex", imageIndex);
                intentDelete.putExtra("imageDate", imageDate);
                // add by Quan, xác định link ảnh cần xóa
                intentDelete.putExtra("imageLink",imagePath);
                //
                sendBroadcast(intentDelete);
                finish();
            }
        });

        // gọi lệnh Show để hiện Dialog
        dialog.show();
    }


    // Xử lý hiện dialog xác nhận xóa ảnh khỏi thùng rác (xóa vĩnh viễn)
    private void openDialogDeleteTrash(String imageIndex)
    {
        final Dialog dialog= new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_delete_trash);

        Window window= dialog.getWindow();
        if(window==null)
        {
            return;
        }

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams windowAttributes= window.getAttributes();
        windowAttributes.gravity= Gravity.BOTTOM;
        window.setAttributes(windowAttributes);
        dialog.setCancelable(true);

        Button btnDialogCancel= dialog.findViewById(R.id.btnDialogCancel);
        Button btnDialogDelete= dialog.findViewById(R.id.btnDialogDelete);

        // Khi nhấn nút Cancel của Dialog
        btnDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Khi nhấn nút Delete của Dialog
        btnDialogDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intentDeleteTrash = new Intent("deleteTrash");
                intentDeleteTrash.putExtra("imageIndexTrash", imageIndex);
                // THêm bởi quân, xác định link ảnh cần xóa vĩnh viễn khỏi trash
                intentDeleteTrash.putExtra("linkImage", imagePath);
                // xong
                sendBroadcast(intentDeleteTrash);
                finish();
            }
        });

        // gọi lệnh Show để hiện Dialog
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Ảnh sau khi cắt xong sẽ trả về result code == 101
        // Ứng với request code == 100 khi start editIntent (btnEdit.onClick)
        if (requestCode == 100 && resultCode == 101) {

            // Nhận kết quả ảnh từ hoạt động ImageCropper
            String result = data.getStringExtra("Crop");
            Uri resultUri = null;

            Bitmap bitmap = null;

            if (result != null) {
                resultUri = Uri.parse(result);

                // Lưu ảnh
                saveImage(resultUri);

                try {
                    Bitmap bmEditedImage = BitmapFactory.decodeStream(
                            getContentResolver().openInputStream(resultUri));
                    if (bmEditedImage != null) {

                        // Tạo một ImageActivity mới để hiển thị ảnh vừa chỉnh
                        String dateTaken = editedImage.getDateTaken();
                        Bundle mybundle = new Bundle();
                        mybundle.putString("imageLink", result);
                        mybundle.putString("imageDate", dateTaken);
                        mybundle.putString("imageIndex", String.valueOf(editedImage.getId()));
                        mybundle.putString("footer", "1");

                        Intent intent = new Intent(this, ImageActivity.class);
                        intent.putExtra("package", mybundle);
                        startActivity(intent);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveImage(Uri uri) {
        //*** Tạo bitmap và nén nó rồi lưu xuống bộ nhớ bằng file stream
        // Sau khi lưu thì tạo một image Model để truyền vào và bắt đầu một hoạt động xem ảnh vừa chỉnh sửa
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Tạo tên cho ảnh mới là chuỗi kí tự ngày, giờ chụp chính xác đến milisecond
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            Date now = new Date();
            String fileName = formatter.format(now) + "_cropped" + ".jpg";

            // Tạo đường dẫn để lưu ảnh
            // Tạo thư mục có tên Album app trong thư mục Pictures của bộ nhớ điện thoại
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AlbumApp");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Tạo file output stream và nén bitmap rồi lưu
            File file = new File(directory, fileName);
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, new String[]{"image/jpeg"}, null);

            // Khởi tạo ảnh để truyền vào và bắt đầu một hoạt động xem ảnh vừa chỉnh sửa
            editedImage = new imageModel(0, formatter.toString(), uri);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
