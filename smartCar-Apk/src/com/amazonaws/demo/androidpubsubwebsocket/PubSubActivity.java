/**
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.androidpubsubwebsocket;

import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;

public class PubSubActivity extends Activity {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a1k03ubnoqnufz-ats.iot.us-east-1.amazonaws.com";

    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-east-1:c0e05020-eb41-48f7-bed2-19d1acc4d222";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;

    Timer timer;

    TextView tvStatus;

    Button btnConnect;
    Button btnDisconnect;

    static AWSIotMqttManager mqttManager;
    String clientId;

    CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);

        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(disconnectClick);

        // MQTT client IDs are required to be unique per AWS IoT account.
        clientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

    }

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    tvStatus.setText("Connecting...");

                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    tvStatus.setText("Connected");

                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                        throwable.printStackTrace();
                                    }
                                    tvStatus.setText("Disconnected");
                                } else {
                                    tvStatus.setText("Disconnected");

                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }
            timer= new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent move = new Intent(PubSubActivity.this,movement.class);
                    startActivity(move);
                }
            },2000);

        }
    };

//    View.OnClickListener subscribeClick = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//
//            final String topic = txtSubscribe.getText().toString();
//
//            Log.d(LOG_TAG, "topic = " + topic);
//
//            try {
//                mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
//                        new AWSIotMqttNewMessageCallback() {
//                            @Override
//                            public void onMessageArrived(final String topic, final byte[] data) {
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        try {
//                                            String message = new String(data, "UTF-8");
//                                            Log.d(LOG_TAG, "Message arrived:");
//                                            Log.d(LOG_TAG, "   Topic: " + topic);
//                                            Log.d(LOG_TAG, " Message: " + message);
//
//                                            tvLastMessage.setText(message);
//
//                                        } catch (UnsupportedEncodingException e) {
//                                            Log.e(LOG_TAG, "Message encoding error.", e);
//                                        }
//                                    }
//                                });
//                            }
//                        });
//            } catch (Exception e) {
//                Log.e(LOG_TAG, "Subscription error.", e);
//            }
//        }
//    };

    View.OnClickListener disconnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                mqttManager.disconnect();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Disconnect error.", e);
            }

        }
    };
}
