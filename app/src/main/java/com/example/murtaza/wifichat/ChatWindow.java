package com.example.murtaza.wifichat;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ChatWindow extends AppCompatActivity {

    private static final int MESSAGE_READ = 1;
    SendReceive sendReceive;
    LinearLayout chatScroll;
    LayoutInflater inflater;
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        Socket socket = SocketHandler.getSocket();

        final SendReceive sendReceive = new SendReceive(socket);
        sendReceive.start();

        final TextView enterMessage = findViewById(R.id.enterMessage);
        scrollView = findViewById(R.id.scrollView);

        chatScroll = findViewById(R.id.chat_scroll);
        inflater =  (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        ImageView sendMessage = findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enterMessage != null && !enterMessage.getText().toString().equals("")) {
                    final String msg = enterMessage.getText().toString();

                    //
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendReceive.write(msg.getBytes());
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    // Stuff that updates the UI
                                    updateUI("message", enterMessage.getText().toString());
                                }
                            });
                        }
                    });
                    thread.start();
                }
                scrollToBottom();
            }
        });

    }


    private void scrollToBottom(){
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String response_msg = new String(readBuff, 0, msg.arg1);
//                    readMsgBox.append("\n"+tempMessage);
                    updateUI("response", response_msg);
                    scrollToBottom();
                    break;
            }
            return true;
        }
    });

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket socket){
            this.socket = socket;
            try {
                this.inputStream = socket.getInputStream();
                this.outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes_len;

            while (socket != null){
                try {
                    bytes_len = inputStream.read(buffer);
                    if(bytes_len > 0){
                        handler.obtainMessage(MESSAGE_READ, bytes_len, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] messageBytes){
            try {
                outputStream.write(messageBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateUI(String type, String message) {
        View view = inflater.inflate(R.layout.chat, null);
        if (type.equals("message")) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.START;
            params.bottomMargin = 12;
            params.rightMargin = 40;
            view.setLayoutParams(params);
            TextView enterMessage = findViewById(R.id.enterMessage);
            enterMessage.setText("");
            TextView text = view.findViewById(R.id.message);
            text.setText(message);
            text.setPadding(0,0,10, 0);
            view.setBackgroundColor(0xffffffff);
            chatScroll.addView(view);
        } else if (type.equals("response")) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.END;
            params.bottomMargin = 12;
            params.leftMargin = 40;
            view.setLayoutParams(params);
            TextView text = view.findViewById(R.id.message);
            text.setText(message);
            text.setPadding(10, 0, 0, 0);
            view.setBackgroundColor(0xfaedb000);
            chatScroll.addView(view);
        } else if (type.equals("error")) {
            TextView text = view.findViewById(R.id.message);
            message = "error"+message;
            text.setText(message);
            view.setBackgroundColor(0x22222222);
            chatScroll.addView(view);
        }
        Date currentTime = Calendar.getInstance().getTime();
        DateFormat df = new SimpleDateFormat("h:mm a");
        String date = df.format(currentTime);
        TextView text = view.findViewById(R.id.time);
        text.setText(date);
    }
}
