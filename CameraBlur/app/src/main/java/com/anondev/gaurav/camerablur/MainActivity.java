package com.anondev.gaurav.camerablur;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.ReturnMode;
import com.esafirm.imagepicker.model.Image;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private class InitializeModelAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean ret=true;
            if(!DeeplabProcessor.isInitialized())
                ret = DeeplabProcessor.initialize(MainActivity.this);

            return ret;
        }

    }
    Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    IntentFilter statusIntentFilter = new IntentFilter(
            "PotraitFinished");
    private MyResponseReceiver responseReceiver =
            new MyResponseReceiver();
    private DragListView mDragListView;
    private GridItemAdapter mDragAdapter;
    private ArrayList<Pair<Integer,String>> mItemList;

    private ArrayList<Integer> multiSelectList;
    private Dbhandler dbhandler;
    private int ID;
    FragmentManager fragmentManager;
    SlideShowDialogFragment newFragment;
    private int Request_for_Points=111,Request_for_images=12321;
    private int spanCount=2;
    private boolean mIsRearrangeOn=false,mIsDragging=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new InitializeModelAsyncTask().execute();//init The Processor
        mDragListView=findViewById(R.id.drag_list_view);
        mDragListView.getRecyclerView().setVerticalScrollBarEnabled(true);
        mDragListView.setPadding(0,12,0,0);
        mDragListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragStarted(int position) {
                mIsDragging=true;
            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                if (fromPosition != toPosition) {
                    dbhandler.move_entry(fromPosition,toPosition);
                }
                mIsDragging=false;
                mDragAdapter.notifyDataSetChanged();
            }
        });
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        dbhandler=new Dbhandler(this,null,null,1);

        fragmentManager=getSupportFragmentManager();
        newFragment = SlideShowDialogFragment.newInstance();
        SetUpGridView();
        FloatingActionButton fab=findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Show PopUp with choice camera or storage
                check_permissions();

            }
        });

    }
    private void check_permissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askPermission();

        } else {
            ShowPopUp();
            // write your logic here
        }
    }
    void ShowPopUp(){
        CharSequence Choice[] = new CharSequence[] {"From storage", "From Camera"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("From");
        builder.setItems(Choice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // the user clicked on colors[which]
                if(which==0){
                    ImagePicker.create(MainActivity.this)
                            .returnMode(ReturnMode.NONE)
                            .folderMode(true)
                            .toolbarFolderTitle("Folder")
                            .toolbarImageTitle("Tap to select")
                            .showCamera(false)
                            .single()
                            .theme(R.style.ImagePickerTheme)
                            .enableLog(false)
                            .start();
                }else if(which==1){
                   Intent intent=new Intent(MainActivity.this,OpenCamera.class);
                    intent.putExtra("option",OpenCamera.Req_for_Image);
                    startActivityForResult(intent,OpenCamera.Req_for_Image);
                }
            }
        });
        builder.show();
    }
    private void SetUpGridView(){

        spanCount=2;
        if(Resources.getSystem().getDisplayMetrics().widthPixels>Resources.getSystem().getDisplayMetrics().heightPixels)
        {
            spanCount=3;
        }


        mDragListView.getRecyclerView().setClipToPadding(false);
        mDragListView.getRecyclerView().setPadding(8,0,8,8);
        mDragListView.setLayoutManager(new GridLayoutManager(this,spanCount));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(MainActivity.this, R.dimen.item_offset);
        mDragListView.getRecyclerView().addItemDecoration(itemDecoration);
        mDragListView.setCanDragHorizontally(true);
        mDragListView.setDragEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        mItemList=new ArrayList<>(dbhandler.rCount());
        for (int i=0;i<dbhandler.rCount();i++){
            mItemList.add(new Pair<Integer, String>(i,dbhandler.getresult(i).path));
        }
        mDragAdapter=new GridItemAdapter(mItemList,R.layout.grid_item,R.id.grid_item_layout,true,MainActivity.this);
        mDragListView.setAdapter(mDragAdapter,false);
        CheckIfListisEmpty();
    }
    private void UpdateList(){

        mItemList=new ArrayList<>(dbhandler.rCount());
        for (int i=0;i<dbhandler.rCount();i++){
            mItemList.add(new Pair<Integer, String>(i,dbhandler.getresult(i).path));
        }
        mDragAdapter=new GridItemAdapter(mItemList,R.layout.grid_item,R.id.grid_item_layout,true,MainActivity.this);
        mDragListView.setAdapter(mDragAdapter,false);
        mDragAdapter.notifyDataSetChanged();
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        CheckIfListisEmpty();
    }
    private void CheckIfListisEmpty(){
        if(mDragListView.getAdapter().getItemCount()>0){
            mDragListView.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.empty_view)).setVisibility(View.GONE);
        }else {
            mDragListView.setVisibility(View.GONE);
            ((TextView)findViewById(R.id.empty_view)).setVisibility(View.VISIBLE);
        }
    }


    public void multi_select(int position) {
        if(multiSelectList.contains(position)){
            multiSelectList.remove(Integer.valueOf(position));
        }else {
            multiSelectList.add(position);
        }
        invalidateOptionsMenu();
        getSupportActionBar().setTitle(multiSelectList.size() + "/" + dbhandler.rCount());
        mDragAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==OpenCamera.Req_for_Image&&resultCode==RESULT_OK){
            final Context context = getBaseContext();
            Intent intent = new Intent(context, PortraitBlurService.class);
            intent.putExtra("path",data.getStringExtra("path"));
            startService(intent);
            UpdateList();
        }
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            // Get a list of picked images
            Image image = ImagePicker.getFirstImageOrNull(data);
            String srcPath=image.getPath();


                final Context context = getBaseContext();
                Intent intent = new Intent(context, PortraitBlurService.class);
                intent.putExtra("path",srcPath);
                startService(intent);
                UpdateList();


            return;

        }
    }

    public void SetMultiToolbar(){
        multiSelectList=new ArrayList<>();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(0+"/"+dbhandler.rCount());
        invalidateOptionsMenu();
    }
    public void StartFragment(int position){
        try {
            newFragment.onDestroy();
        }catch (Exception e){
        }
        //getSupportActionBar().hide();
        newFragment=SlideShowDialogFragment.newInstance();
        FragmentTransaction ft=fragmentManager.beginTransaction();
        Bundle bundle=new Bundle();
        String paths[]=new String[dbhandler.rCount()];
        for(int i=0;i<paths.length;i++){
            paths[i]=dbhandler.getresult(i).getPath();
        }
        bundle.putSerializable("path",paths);
        bundle.putInt("position",position);
        bundle.putInt("total",paths.length);
        newFragment.setArguments(bundle);
        newFragment.onCreate(bundle);
        newFragment.show(ft, "slideshow");
    }

    @Override
    public void onBackPressed() {
        if(mDragAdapter.mIsMultiSelect)
        {
            mDragAdapter.mIsMultiSelect=false;
            resetToolbar();
        }else super.onBackPressed();

    }
    public void resetToolbar(){
        mDragAdapter.mIsMultiSelect=false;
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        mDragListView.setDragEnabled(false);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        invalidateOptionsMenu();
        mIsRearrangeOn=false;
        mDragAdapter.notifyDataSetChanged();
    }

    private class GridItemAdapter extends DragItemAdapter<Pair<Integer,String>,GridItemAdapter.ViewHolder> {
        private int mLayoutId;
        private int mGrabHandleId;
        private boolean mDragOnLongPress;
        private int viewHeight;
        public boolean mIsMultiSelect=false;
        private Context mContext;
        GridItemAdapter(ArrayList<Pair<Integer,String>> list, int layoutId, int grabHandleId, boolean dragOnLongPress, Context context){
            mContext=context;
            mLayoutId = layoutId;
            mGrabHandleId = grabHandleId;
            mDragOnLongPress = dragOnLongPress;
            mContext=context;
            setItemList(list);
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
            if(parent.getMeasuredHeight()>parent.getMeasuredWidth()) {
                viewHeight =(int)((parent.getMeasuredWidth()/spanCount)*4*1.0/3);
            }
            else  viewHeight =(int)((parent.getMeasuredWidth()/spanCount)*4*1.0/3);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            holder.mFrameLayout.getLayoutParams().height=viewHeight;
            holder.mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if(mIsMultiSelect)
            {
                holder.mCheckBox.setVisibility(View.VISIBLE);
                if(multiSelectList.contains(position))
                    holder.mCheckBox.setChecked(true);
                else holder.mCheckBox.setChecked(false);
            }else {
                holder.mCheckBox.setVisibility(View.GONE);
            }
            holder.mTextView.setText(""+(position+1));
            holder.mTextView.setWidth(holder.mFrameLayout.getWidth());
            Glide.with(mContext).load(dbhandler.getresult(position).path).apply(RequestOptions.bitmapTransform(new com.bumptech.glide.load.resource.bitmap.CenterCrop() )).into(holder.mImageView);

        }

        @Override
        public long getUniqueItemId(int position) {
            return getItemList().get(position).first;
        }

        class ViewHolder extends DragItemAdapter.ViewHolder {
            ImageView mImageView;
            TextView mTextView;
            FrameLayout mFrameLayout;
            CheckBox mCheckBox;
            ViewHolder(final View itemView) {
                super(itemView,  mGrabHandleId, mDragOnLongPress);
                mFrameLayout=(FrameLayout)itemView.findViewById(R.id.grid_item_layout);
                mImageView=(ImageView)itemView.findViewById(R.id.grid_item_imageView);
                mTextView=(TextView)itemView.findViewById(R.id.grid_item_textView);
                mCheckBox=(CheckBox)itemView.findViewById(R.id.grid_item_checkbox);
                mCheckBox.setVisibility(View.GONE);
            }

            @Override
            public void onItemClicked(View view) {

                //start fragment from its position
                if(mIsMultiSelect){
                    mCheckBox.setVisibility(View.VISIBLE);
                    multi_select(getAdapterPosition());
                }else{
                    StartFragment(getAdapterPosition());
                    //call fragment
                }

            }

            @Override
            public boolean onItemLongClicked(View view) {
                //start multiselect toolbar function
                super.onItemLongClicked(view);
                if(!mIsMultiSelect) {
                    mIsMultiSelect = true;
                    SetMultiToolbar();
                }
                multi_select(getAdapterPosition());
                return true;
            }
        }
    }
    class ItemOffsetDecoration extends RecyclerView.ItemDecoration {

        private int mItemOffset;

        public ItemOffsetDecoration(int itemOffset) {
            mItemOffset = itemOffset;
        }

        public ItemOffsetDecoration(Context context,int itemOffsetId) {
            this(context.getResources().getDimensionPixelSize(itemOffsetId));
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.set(mItemOffset, mItemOffset, mItemOffset, mItemOffset);
        }
    }
    void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }

        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }


    }
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.responseReceiver);
    }
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(responseReceiver, statusIntentFilter );
        mDragAdapter.notifyDataSetChanged();
        if(newFragment!=null &&  newFragment.getDialog()!=null
                && newFragment.getDialog().isShowing()){
            String paths[]=new String[dbhandler.rCount()];
            for(int i=0;i<paths.length;i++){
                paths[i]=dbhandler.getresult(i).getPath();
            }
            MainActivity.this.newFragment.Update(paths);
        }
    }
    private class MyResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!mIsDragging) {
                UpdateList();
                mDragAdapter.notifyDataSetChanged();
                if(newFragment!=null &&  newFragment.getDialog()!=null
                        && newFragment.getDialog().isShowing()){
                    String paths[]=new String[dbhandler.rCount()];
                    for(int i=0;i<paths.length;i++){
                        paths[i]=dbhandler.getresult(i).getPath();
                    }
                    MainActivity.this.newFragment.Update(paths);
                }
            }
        }
    }
    private   static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    private void askPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED|| ContextCompat
                .checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (this, Manifest.permission.CAMERA)) {

                Snackbar.make(this.findViewById(android.R.id.content),
                        "Please Grant Permissions to store images",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                    requestPermissions(
                                            new String[]{Manifest.permission
                                                    .READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                                            PERMISSIONS_MULTIPLE_REQUEST);
                            }
                        }).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    requestPermissions(
                            new String[]{Manifest.permission
                                    .READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                            PERMISSIONS_MULTIPLE_REQUEST);
            }
        } else {
           ShowPopUp();
            // write your logic code if permission already granted
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean cameraPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(cameraPermission && readExternalFile)
                    {
                        ShowPopUp();
                        // write your logic here
                    } else {
                        Snackbar.make(this.findViewById(android.R.id.content),
                                "Please Grant Permissions to enable Camera",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                            requestPermissions(
                                                    new String[]{Manifest.permission
                                                            .READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                                                    PERMISSIONS_MULTIPLE_REQUEST);
                                    }
                                }).show();
                    }
                }
                break;
        }
    }
}
