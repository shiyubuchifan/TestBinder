package cn.snow.testbinder;

import static android.content.ContentValues.TAG;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

import cn.snow.interviewapp.aidl.IPCTestBean;
import cn.snow.interviewapp.aidl.IaidlSnowInterface;
import cn.snow.testbinder.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    IaidlSnowInterface iaidlSnow;

    ServiceConnection ipcConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            iaidlSnow = IaidlSnowInterface.Stub.asInterface(service);

            StringBuilder sb = new StringBuilder();
            sb.append("IPC onServiceConnected\n");
            try {
                sb.append("My Main Thread Id = ").append(Thread.currentThread().getId()).append("\n").append("My Remote Process Id = ").append(iaidlSnow.getProcessId()).append("\n").append("My Remote Process Name = ").append(iaidlSnow.getProcessName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            binding.tvMain.setText(sb.toString());

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    Socket socket = new Socket();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);


        binding.tvMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


            }
        });

        binding.btnIpc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("cn.snow.interview.service.ipc");//IPC Service 的action
                intent.setPackage("cn.snow.interviewapp");//设置app包名
                bindService(intent, ipcConnection, BIND_AUTO_CREATE);
            }
        });

        binding.btnIpcClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iaidlSnow != null) {
                    unbindService(ipcConnection);
                    iaidlSnow = null;
                    binding.tvMain.setText("IPC unbindService");
                    return;
                }
                binding.tvMain.setText("IPC No Bind");
            }
        });

        binding.btnProvider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initData();

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {

                            if (!socket.isConnected()) {
                                socket.connect(new InetSocketAddress(12188));
                            }
                            InputStream inputStream = socket.getInputStream();
                            OutputStream outputStream = socket.getOutputStream();
                            outputStream.write("我是少先队员，北京天安门上红旗飘\n 你好啊北京！\t qaq".getBytes());

//                            outputStream.close();
//                            inputStream.close();
//                            socket.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });

        binding.btnIpcParcelGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iaidlSnow != null) {
                    try {
                        binding.tvMain.setText(iaidlSnow.getIPCTestBean() == null ? "null" : iaidlSnow.getIPCTestBean().toString());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        binding.btnIpcParcelSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iaidlSnow != null) {
                    IPCTestBean bean = new IPCTestBean();
                    bean.setAge(18);
                    bean.setName("TestBinder App");

                    try {
                        iaidlSnow.setIPCTestBean(bean);
                        binding.tvMain.setText("Set Parcel Data.");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        binding.btnIpcParcelSetMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mServerMessenger == null) {

                    //绑定
                    Intent intent = new Intent();
                    intent.putExtra("type", 1);
                    intent.setAction("cn.snow.interview.service.ipc");//IPC Service 的action
                    intent.setPackage("cn.snow.interviewapp");//设置app包名
                    bindService(intent, messagerServiceConnection, BIND_AUTO_CREATE);

                    return;
                }


                //测试一下能否设置数据
                Message test = Message.obtain(null, MSG_SET_VALUE);
                Bundle bundle = new Bundle();
                bundle.putString("data", "Test App" + new Random().nextInt());
                test.setData(bundle);
                try {
                    if (mServerMessenger != null) mServerMessenger.send(test);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        binding.btnIpcParcelCloseMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(messagerServiceConnection);
            }
        });

    }

    private ServiceConnection messagerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            //服务端的Messenger
            mServerMessenger = new Messenger(service);

            //现在开始构client用来传递和接收消息的messenger
            Messenger clientMessenger = new Messenger(new ClientHandler());
            try {
                //将client注册到server端
                Message register = Message.obtain(null, MSG_REGISTER_CLIENT);
                register.replyTo = clientMessenger;//这是注册的操作，我们可以在上面的Server代码看到这个对象被取出
                mServerMessenger.send(register);

                Toast.makeText(MainActivity.this, "绑定成功", Toast.LENGTH_SHORT).show();

            } catch (RemoteException e) {
                e.printStackTrace();
            }


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    private void initData() {

        Uri uri = Uri.parse("content://cn.snow.interviewapp.provider.test/user");

//        ContentValues values = new ContentValues();
//        values.put("_id", 3);
//        values.put("name", "Tom");
        // 获取ContentResolver
        ContentResolver resolver = getContentResolver();
        // 通过ContentResolver 根据URI 向ContentProvider中插入数据
//        resolver.insert(uri, values);
        // 通过ContentResolver 向ContentProvider中查询数据
        Cursor cursor = resolver.query(uri, new String[]{"_id", "name"}, null, null, null);
        StringBuilder b = new StringBuilder();
        try {
            while (cursor.moveToNext()) {
                System.out.println("query user:" + cursor.getInt(0) + " " + cursor.getString(1));
                // 将表中数据全部输出
                b.append("query user:").append(cursor.getInt(0)).append(" ").append(cursor.getString(1)).append("\n");
            }

            uri = Uri.parse("content://cn.snow.interviewapp.provider.test/age");

            cursor = resolver.query(uri, new String[]{"_id", "age"}, null, null, null);

            while (cursor.moveToNext()) {
                System.out.println("query age:" + cursor.getInt(0) + " " + cursor.getString(1));
                // 将表中数据全部输出
                b.append("query age:").append(cursor.getInt(0)).append(" ").append(cursor.getString(1)).append("\n");
            }

            binding.tvMain.setText(b.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().getData() != null) {
            Log.e(TAG, "onResume: " + getIntent().getData().getScheme() + "-" + getIntent().getData().getHost() + "-"+ getIntent().getData().getPath());
            Log.e(TAG, "onResume: " + getIntent().getType());

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(getIntent().getData());
            startActivity(intent);

        }
    }

    //这些类型要和Server端想对应
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_VALUE = 3;
    static final int MSG_CLIENT_SET_VALUE = 4;

    Messenger mServerMessenger;

    class ClientHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CLIENT_SET_VALUE) {
//                binding.tvMain.setText(((IPCTestBean) msg.obj) == null ? "null" : ((IPCTestBean) msg.obj).toString());
                binding.tvMain.setText(msg.getData().getString("data"));
            } else {
                super.handleMessage(msg);
            }
        }
    }

}