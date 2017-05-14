/*
 * Copyright (c) 2017. Osama Bin Omar
 *    This file is part of Crypto File Manager also known as Crypto FM
 *
 *     Crypto File Manager is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Crypto File Manager is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Crypto File Manager.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.osama.cryptofmroot.filemanager.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.osama.RootTools.RootTools;
import com.osama.cryptofmroot.CryptoFM;
import com.osama.cryptofmroot.R;
import com.osama.cryptofmroot.about.AboutActivity;
import com.osama.cryptofmroot.extras.KeyDetailsActivity;
import com.osama.cryptofmroot.extras.TextEditorActivity;
import com.osama.cryptofmroot.filemanager.listview.AdapterCallbacks;
import com.osama.cryptofmroot.filemanager.utils.FragmentCallbacks;
import com.osama.cryptofmroot.filemanager.utils.TabsPagerAdapter;
import com.osama.cryptofmroot.filemanager.utils.SharedData;
import com.osama.cryptofmroot.filemanager.utils.UiUtils;
import com.osama.cryptofmroot.root.RootUtils;
import com.osama.cryptofmroot.services.CleanupService;
import com.osama.cryptofmroot.tasks.BackupKeysTask;
import com.osama.cryptofmroot.utils.ActionHandler;
import com.osama.cryptofmroot.utils.CommonConstants;
import com.osama.cryptofmroot.utils.FileUtils;

import java.util.ArrayList;

public class FileManagerActivity extends AppCompatActivity implements AdapterCallbacks,
        FragmentCallbacks, FloatingActionsMenu.OnFloatingActionsMenuUpdateListener{
    private int                     mTotalStorage;
    private boolean                 isEmptyFolder=false;
    private  TabsFragmentOne         mCurrentFragment;
    private ArrayList<String>       mStoragePaths;
    private TabsFragmentOne[]       mFragmentOnes;
    private ArrayList<String>       mStorageTitles;
    private static boolean          isServiceStarted=false;
    private FloatingActionsMenu     mFloatingActionsMenu;
    private static final String TAG=FileManagerActivity.class.getName();
    private boolean viewChanged=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStoragePaths =new ArrayList<>();
        mStorageTitles =new ArrayList<>();
        //check root access
        if(RootTools.isRootAvailable()) {
            if (RootTools.isAccessGiven()) {
                Log.d(TAG, "onCreate: Root access is available and given");
                mStoragePaths.add("/");
                mStoragePaths.add(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
                mStorageTitles.add("root");
                mStorageTitles.add("home");
            } else {
                Log.d(TAG, "onCreate: Root access is not available");
                rootUnavailableOp();
            }
        }else{
            rootUnavailableOp();
        }
        setContentView(R.layout.activity_filemanager_tabs);
        SharedData.STARTED_IN_SELECTION_MODE=false;
        SharedData.DO_NOT_RESET_ICON=false;
        SharedPreferences prefs=getSharedPreferences("done",Context.MODE_PRIVATE);
        SharedData.KEYS_GENERATED=prefs.getBoolean("keys_gen",false);
        SharedData.SIMPLE_VIEW_MODE=prefs.getBoolean(CommonConstants.VIEW_MODE_SHARED_PREF,false);
        if(!isServiceStarted){
            startService(new Intent(CryptoFM.getContext(),CleanupService.class));
            isServiceStarted=true;
        }
        //see the external dirs

        mTotalStorage = mStoragePaths.size();
        if(SharedData.DB_PASSWORD==null ){
            SharedData.DB_PASSWORD  = getIntent().getExtras().getString("dbpass");
            SharedData.USERNAME	    = getIntent().getExtras().getString("username","default");
        }
        SharedData.GRID_LAYOUTMANAGER=getPreferences(Context.MODE_PRIVATE).getBoolean("layout",true);
        setToolbar();
        mFloatingActionsMenu=(FloatingActionsMenu)findViewById(R.id.fab_add_folder);
        mFloatingActionsMenu.setOnFloatingActionsMenuUpdateListener(this);

    }

    private void rootUnavailableOp() {
            mStoragePaths.add(Environment.getExternalStorageDirectory().getAbsolutePath()+"/");
            mStoragePaths.add(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/");
            mStorageTitles.add("home");
            mStorageTitles.add("download");
    }

    @ActionHandler(layoutResource = R.id.fab_add_folder)
    public void onAddFloatingClicked(View v){
        UiUtils.actionMode = this.actionMode;
        mFloatingActionsMenu.collapse();
        if(mCurrentFragment==null){
            Log.d(TAG, "onAddFloatingClicked: fragment is null ");
            mCurrentFragment=mFragmentOnes[0];
        }
        if(isEmptyFolder) {
            removeNoFilesFragment();
        }
        final Dialog dialog = UiUtils.createDialog(
                this,
                "Create Folder",
                "create"
        );

        final EditText folderEditText = (EditText)dialog.findViewById(R.id.foldername_edittext);
        Button okayButton			  = (Button)dialog.findViewById(R.id.create_file_button);

        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String folderName=folderEditText.getText().toString();
                if(folderName.length()<1){
                    folderEditText.setError("Give me the folder name");
                }else if(RootUtils.isRootPath(mCurrentFragment.getmCurrentPath())){
                    RootUtils.createFolder(mCurrentFragment.getmCurrentPath()+folderName);
                    dialog.dismiss();
                    UiUtils.reloadData(mCurrentFragment.getmFileAdapter());

                }
                else if(!FileUtils.createFolder(mCurrentFragment.getmCurrentPath()+folderName)){
                        Toast.makeText(
                                FileManagerActivity.this,
                                "Cannot create folder make sure current path is writable",
                                Toast.LENGTH_SHORT
                        ).show();
                    }else{
                        dialog.dismiss();
                    //getContentResolver().insert(FileUtils.getUri(mCurrentFragment.getmCurrentPath()+folderName),null);
                    //FileUtils.notifyChange(TextEditorActivity.this,mFile.getParent());
                    FileUtils.notifyChange(FileManagerActivity.this,mCurrentFragment.getmCurrentPath()+folderName);
                        UiUtils.reloadData(
                                mCurrentFragment.getmFileAdapter()
                        );
                    }
            }
        });
    }
    @ActionHandler(layoutResource = R.id.createFilebutton)
    public void onCreateFileButtonClick(View view){
        mFloatingActionsMenu.collapse();
        //start editor activity
        Intent intent=new Intent(this,TextEditorActivity.class);
        if(mCurrentFragment==null){
            mCurrentFragment=mFragmentOnes[0];
        }
        intent.putExtra(CommonConstants.TEXTEDITACT_PARAM_PATH,mCurrentFragment.getmCurrentPath());
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(SharedData.IS_IN_COPY_MODE ){
            getMenuInflater().inflate(R.menu.copy_menu,menu);
            return true;
        }
        getMenuInflater().inflate(R.menu.appbar_menu, menu);
        Log.d(TAG, "onCreateOptionsMenu: menu created");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu: preparing menu");
        if(SharedData.IS_IN_COPY_MODE){
            return super.onPrepareOptionsMenu(menu);
        }
        if(menu.size()>2) {
            MenuItem item = menu.getItem(0);
            if (SharedData.GRID_LAYOUTMANAGER) {
                item.setIcon(getDrawable(R.drawable.ic_gridview));
            }
            if (SharedData.SIMPLE_VIEW_MODE) {
                menu.getItem(2).setTitle("Card view");
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //change the current path in fragment
        if(mCurrentFragment==null){
            mCurrentFragment=mFragmentOnes[0];
        }
        if(SharedData.IS_IN_COPY_MODE){
            if(item.getItemId()==R.id.cancel_menu_item){
                SharedData.IS_IN_COPY_MODE=false;
                invalidateOptionsMenu();
                openOptionsMenu();
            }else if(item.getItemId()==R.id.paste_here_menu_item){
                SharedData.IS_IN_COPY_MODE=false;
                invalidateOptionsMenu();
                openOptionsMenu();
                removeNoFilesFragment();
                mCurrentFragment.executeCopyTask();

            }
            return true;
        }
        if(item.getItemId()==R.id.open_source_menu_item){
            startActivity(new Intent(this,AboutActivity.class));
        }else if(item.getItemId()==R.id.backup_menu_item) {
                showBackupDialog();
        }else if (item.getItemId()==R.id.refresh_menu_item){
            UiUtils.reloadData(mCurrentFragment.getmFileAdapter());
        }else if(item.getItemId()==R.id.viewmode_menu_item){
            mCurrentFragment.toggleViewMode();
            viewChanged=true;
            if(SharedData.SIMPLE_VIEW_MODE){
                item.setTitle("Card view");
            }else{
                item.setTitle("Simple view");
            }

        }
        else if(item.getItemId()==R.id.items_view_menu_item){
            SharedData.GRID_LAYOUTMANAGER=!SharedData.GRID_LAYOUTMANAGER;
        mCurrentFragment.toggleLayout(item);
        }else if(item.getItemId()==R.id.keydetails_menu_item){
            startActivity(new Intent(this, KeyDetailsActivity.class));
        }

        return true;
    }

    private void showBackupDialog() {
      final Dialog dialog=new Dialog(this);
        dialog.setContentView(R.layout.password_dialog_layout);
        Button button=(Button)dialog.findViewById(R.id.decrypt_file_button);
        button.setText("Backup");
        ((TextInputLayout)dialog.findViewById(R.id.key_password_layout)).setHint("Application password");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pass=((EditText)dialog.findViewById(R.id.key_password)).getText().toString();
                if(pass.length()<1){
                    Toast.makeText(FileManagerActivity.this,"Please input application password",Toast.LENGTH_LONG).show();
                    return;
                }
                new BackupKeysTask().execute(pass);
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.cancel_decrypt_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    @Override
    public void onBackPressed() {
        //change the current path in fragment
        if(mCurrentFragment==null){
            mCurrentFragment=mFragmentOnes[0];
        }
        if(isEmptyFolder) {
            removeNoFilesFragment();
        }
        String path=mCurrentFragment.getmCurrentPath();
        Log.d(TAG, "onBackPressed: Current path is: "+path);
        if(path.equals(mCurrentFragment.getRootPath())){
            super.onBackPressed();
        }else{
            path		   = path.substring(0,path.lastIndexOf('/'));
            path 		   = path.substring(0,path.lastIndexOf('/')+1);
            Log.d(TAG, "onBackPressed: Changing directory");
            changeTitle(path);
            mCurrentFragment.changeDirectory(path,1);
        }

    }

    @Override
    public void init() {

    }

    @Override
    public void finishActionMode() {
        if(this.actionMode!=null){
            this.actionMode.finish();
            this.actionMode=null;
        }else {
            SharedData.SELECTION_MODE=false;
        }
    }

    @Override
    public void tellNoFiles() {
        showNoFilesFragment();
    }

    @Override
    public void setCurrentFragment(TabsFragmentOne m, int position) {
        Log.d(TAG, "setCurrentFragment: Setting fragments at position: "+position);
        mFragmentOnes[position]=m;
        Log.d(TAG, "setCurrentFragment: fragment at position: "+position+"has path: "+m.getmCurrentPath());
        mFragmentOnes[position].setmCurrentPath(mStoragePaths.get(position));

    }

    private void setToolbar(){
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(mStorageTitles.get(0));
        toolbar.setSubtitle(mStoragePaths.get(0));
        setSupportActionBar(toolbar);


        final ViewPager viewPager  = (ViewPager) findViewById(R.id.pager);
        TabsPagerAdapter mTabsPagerAdapter = new TabsPagerAdapter
                (getSupportFragmentManager(), mStoragePaths, mStorageTitles);
        viewPager.setAdapter(mTabsPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(mCurrentFragment==null){
                    mCurrentFragment=mFragmentOnes[0];
                }
                boolean blur=mCurrentFragment.isBlur();
                //if in copy mode and switching tabs
                ArrayList<String> fileList=null;
                if(SharedData.IS_IN_COPY_MODE){
                    Log.d(TAG, "onPageSelected: Yes im in copying mode but user is switching tab");
                    // get the selected files list
                    fileList= mCurrentFragment.getmTaskHandler().getmSelectedFiles();
                }
                mCurrentFragment=mFragmentOnes[position];
                //again if is in copy mode
                // had to do it twice because the instance of current fragment changes
                if(SharedData.IS_IN_COPY_MODE){
                    Log.d(TAG, "onPageSelected: Setting selected files list: "+fileList.size());
                    mCurrentFragment.getmTaskHandler().setmSelectedFiles(fileList);
                }
                if(actionMode!=null){
                    actionMode.finish();
                    actionMode=null;
                }
                if((mCurrentFragment==null)){
                  return;
                }
                if(mCurrentFragment.ismIsEmptyFolder()){
                    showNoFilesFragment();

                }if(viewChanged){
                    viewChanged=false;
                    mCurrentFragment.reloadDataSet();
                }
                else{
                    removeNoFilesFragment();
                }
                mCurrentFragment.setBlur(!blur);
                mCurrentFragment.toggleBlur();
                changeTitle(mCurrentFragment.getmCurrentPath());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if(mTotalStorage <=1){
            Log.d(TAG, "setToolbar: Tab layout is hiding itself");
            tabLayout.setVisibility(View.GONE);
        }

        mFragmentOnes=new TabsFragmentOne[mTotalStorage];
    }

    /**
     * adapter callbacks section starting
     */


    ActionMode actionMode;
    @Override
    public void onLongClick() {
        //change the current path in fragment
        if(mCurrentFragment==null){
            mCurrentFragment=mFragmentOnes[0];
        }
        actionMode = startActionMode(mCurrentFragment.getmActionViewHandler());
        UiUtils.actionMode=actionMode;
    }
    @Override
    public void incrementSelectionCount(){
        actionMode.setTitle(++SharedData.SELECT_COUNT+"");
        if(SharedData.SELECT_COUNT>1){
            actionMode.getMenu().removeItem(R.id.rename_menu_item);
            actionMode.getMenu().removeItem(R.id.openwith_menu_item);
        }
    }


    @Override
    public void decrementSelectionCount() {
        if(actionMode!=null){
            --SharedData.SELECT_COUNT;
            if(SharedData.SELECT_COUNT==0){
                actionMode.finish();
            }
            else if(SharedData.SELECT_COUNT<2) {
                actionMode.getMenu().add(0, R.id.rename_menu_item, 0, "Rename");
            }
        actionMode.setTitle(SharedData.SELECT_COUNT+"");
        }
    }

    @Override
    public void changeTitle(String path) {
        String tmp=path;
        //change the current path in fragment
        if(mCurrentFragment==null){
            mCurrentFragment=mFragmentOnes[0];
        }
        mCurrentFragment.setmCurrentPath(path);

        if(path.equals(SharedData.FILES_ROOT_DIRECTORY)){
            path="Home";
        }else if(path.equals("/")){
            path="Root";
        }
        else{
            Log.d(TAG, "changeTitle: Path is: "+path);
            path=path.substring(0,path.lastIndexOf('/'));
            path=path.substring(path.lastIndexOf('/')+1);
        }
        assert getSupportActionBar()!=null;
        getSupportActionBar().setTitle(path);
        getSupportActionBar().setSubtitle(tmp);
    }

    @Override
    public void showNoFilesFragment() {
        Log.d("google", "showNoFilesFragment: no files show");
        isEmptyFolder=true;
        FrameLayout layout=(FrameLayout)findViewById(R.id.no_files_frame_fragment);
        View view= getLayoutInflater().inflate(R.layout.no_files_layout,null);
        layout.addView(view);
        mCurrentFragment.setmIsEmptyFolder(true);
    }

    @Override
    public void removeNoFilesFragment() {
        isEmptyFolder=false;
        mCurrentFragment.setmIsEmptyFolder(false);
        FrameLayout layout=(FrameLayout)findViewById(R.id.no_files_frame_fragment);
        layout.removeAllViews();

    }

    @Override
    public void animateForward(String path) {
        mCurrentFragment.changeDirectory(path,0);
    }
    @Override
    public void selectedFileType(boolean isFolder) {
        Log.d(TAG, "selectedFileType: file selected and it it folder? "+isFolder);
        if(isFolder){
            if(SharedData.SELECT_COUNT<2 && !SharedData.IS_OPENWITH_SHOWN){
                SharedData.IS_OPENWITH_SHOWN=true;
                actionMode.getMenu().removeItem(R.id.openwith_menu_item);
            }
        }else{
            if(SharedData.SELECT_COUNT<2 && actionMode.getMenu().findItem(R.id.openwith_menu_item)==null){
                SharedData.IS_OPENWITH_SHOWN=false;
                actionMode.getMenu().add(0,R.id.openwith_menu_item,0,"Open with");
            }
        }
    }

    public void showCopyDialog() {
        actionMode.finish();
        invalidateOptionsMenu();
        openOptionsMenu();

    }

    @Override
    public void onMenuExpanded() {
        if(mCurrentFragment==null) {
            mCurrentFragment=mFragmentOnes[0];
        }
        mCurrentFragment.toggleBlur();

    }

    @Override
    public void onMenuCollapsed() {
        if(mCurrentFragment==null){
            mCurrentFragment=mFragmentOnes[0];
        }
        mCurrentFragment.toggleBlur();
    }
}
