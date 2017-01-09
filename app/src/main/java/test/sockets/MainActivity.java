package test.sockets;

import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.phoenixframework.channels.Channel;
import org.phoenixframework.channels.Envelope;
import org.phoenixframework.channels.IMessageCallback;
import org.phoenixframework.channels.Socket;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {
    @BindView(R.id.responseMessageView)
    TextView responseMessage;
    @BindView(R.id.socketConnectButton)
    Button socketButton;
    @BindView(R.id.sendMsgButton)
    Button sendMsgButton;
    @BindView(R.id.editTextBox)
    EditText editTextBox;

    // TODO: Change the HOST and TOPIC to match the server you are trying to connect to
    private static final String HOST = "ENTER HOST NAME";
    private static final String TOPIC = "ENTER TOPIC";
    private static final String TAG = MainActivity.class.getSimpleName();

    private Socket socket;
    private Channel channel;
    private Envelope currentEnvelope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        socketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectSocket();
            }
        });

        sendMsgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    public void connectSocket() {
        try {
            socket = new Socket(HOST);
            socket.connect();
            channel = socket.chan(TOPIC, null);
            channel.join()
                    .receive("ignore", new IMessageCallback() {
                        @Override
                        public void onMessage(Envelope envelope) {
                            System.out.println("IGNORE");
                        }
                    })
                    .receive("ok", new IMessageCallback() {
                        @Override
                        public void onMessage(Envelope envelope) {
                            System.out.println("JOINED with " + envelope.toString());
                            currentEnvelope = envelope;
                            displayResponse();
                        }
                    });

            channel.on("new:msg", new IMessageCallback() {
                @Override
                public void onMessage(Envelope envelope) {
                    System.out.println("NEW MESSAGE: " + envelope.toString());
                }
            });

            channel.on("new:post", new IMessageCallback() {
                @Override
                public void onMessage(Envelope envelope) {
                    System.out.println("NEW POST: " + envelope.toString());
                    currentEnvelope = envelope;
                    displayResponse();
                }
            });


        } catch (Exception e) {
            Log.e(TAG, "Failed to connect", e);
        }
    }

    private void sendMessage() {
        final String messageBody = editTextBox.getText().toString().trim();
        if (channel != null) {

            //Sending a message. This library uses Jackson for JSON serialization
            ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
                    .put("content", messageBody);

            try {
                channel.push("new:post", node);
            } catch (IOException e) {
                e.printStackTrace();
                showToast(e.toString());
            }
        }
    }

    private void showToast(final String toastText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayResponse() {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                responseMessage.setText(currentEnvelope.toString());
            }
        };
        mainHandler.post(myRunnable);
    }

}
