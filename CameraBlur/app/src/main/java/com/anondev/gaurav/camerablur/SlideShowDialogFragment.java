package com.anondev.gaurav.camerablur;

/**
 * Created by Gaurav on 10/15/2017.
 */

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;/*
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.NavigationView;*/
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.transition.TransitionFactory;
import com.bumptech.glide.request.transition.ViewAnimationFactory;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;



import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import static android.app.Activity.RESULT_OK;

public class SlideShowDialogFragment extends DialogFragment {
    private String TAG = SlideShowDialogFragment.class.getSimpleName();
    private String[] images;
    private int size;
    private ViewPager viewPager;
    public MyViewPagerAdapter myViewPagerAdapter;
    private int selectedPosition = 0;
    public int currentPosition=0;
    ImageButton save;
    Toolbar toolbar;

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    static SlideShowDialogFragment newInstance() {
        SlideShowDialogFragment f = new SlideShowDialogFragment();
        return f;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_image_slider, container, false);
        viewPager = (ViewPager) v.findViewById(R.id.viewpager);
        setHasOptionsMenu(true);
        toolbar=(Toolbar)v.findViewById(R.id.slider_toolbar);
        save=v.findViewById(R.id.save_Item);
        setToolbar();
        //((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        images=getArguments().getStringArray("path");
        selectedPosition = getArguments().getInt("position");
        size=getArguments().getInt("total");
        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setPageTransformer(true,new ZoomOutPageTransformer());
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);
        setCurrentItem(selectedPosition);

        return v;
    }
    private void setToolbar(){
        if(getActivity().getClass()==MainActivity.class)
            save.setVisibility(View.GONE);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity().getClass() == OpenCamera.class) {
                    ((OpenCamera)getActivity()).results_from_camera(images[0]);}
            }
        });
        toolbar.setTitle(R.string.app_name);
    }

 /*   @Override
    public void onResume() {
        super.onResume();
//        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
    }

    @Override
    public void onStop() {
        super.onStop();
        {
            if( getActivity().getClass()==MainActivity.class)
            {
                ((AppCompatActivity) getActivity()).getSupportActionBar().show();
                ((MainActivity)getActivity()).setSupportActionBar(((MainActivity)getActivity()).toolbar);
                ((MainActivity)getActivity()).resetToolbar();
            }
        }
    }

*/



    private void setCurrentItem(final int position) {

        viewPager.setCurrentItem(position, false);
        displayMetaInfo(selectedPosition);
    }

    //  page change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            displayMetaInfo(position);
            currentPosition=position;
        }
        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }
        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    private void displayMetaInfo(int position) {


    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }
    public void Update(String[] newPath){
        images=newPath;
        myViewPagerAdapter.notifyDataSetChanged();
    }

    //  adapter
    public class MyViewPagerAdapter extends PagerAdapter  {

        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.full_screen_image, container, false);
            SubsamplingScaleImageView imageViewPreview = (SubsamplingScaleImageView) view.findViewById(R.id.imgDisplay);

                imageViewPreview.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM);
                imageViewPreview.setMinimumTileDpi(200);

            imageViewPreview.setImage(ImageSource.uri(images[position]));

            imageViewPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggle_bar();
                }
            });
            imageViewPreview.setTag(position);
           view.setTag(position);
            container.addView(view);
            return view;
        }
        void toggle_bar(){

            if(toolbar.getVisibility()==View.VISIBLE)
                toolbar.setVisibility(View.GONE);
            else toolbar.setVisibility(View.VISIBLE);
        }


        @Override
        public int getCount() {
            return size;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == ((View) obj);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }


}