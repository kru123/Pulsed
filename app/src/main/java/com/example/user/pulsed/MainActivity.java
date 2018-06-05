package com.example.user.pulsed;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

public  class MainActivity extends AppCompatActivity implements View.OnClickListener {




  @SuppressLint("HandlerLeak")
  Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch(msg.what){
                case Bluetooth.SUCCESS_CONNECT:
                    Bluetooth.connectedThread=new Bluetooth.ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    String s="Success Connected";
                    Bluetooth.connectedThread.start();
                    break;
                case Bluetooth.MESSAGE_READ:
                    byte[] readBuf =(byte[])msg.obj;
                    String strIncom=new String(readBuf,0,5);
                    if(strIncom.indexOf('s')==0 && strIncom.indexOf('.')==2 ){
                        strIncom=strIncom.replace("s","");
                        if(isFloatNumber(strIncom)){
                            if(graph2lastXValue>=Xview&& Lock == true){
                                series.resetData(new GraphViewData[]{});
                                graph2lastXValue=0;
                            }else graph2lastXValue+=0.1;
                            if(Lock==true)graphView.setViewPort(0,Xview);
                            else graphView.setViewPort(graph2lastXValue-Xview,Xview);
                            GraphView.removeView(graphView);
                            GraphView.addView(graphView);


                        }

                    }
                    break;
            }
        }
        public boolean isFloatNumber(String num){
            try{
                Double.parseDouble(num);
            }catch(NumberFormatException e){
                return false;
            }
            return true;
        }
    };
    Button bConnect,bDisconnect;
    ToggleButton tblock,tbStream;
    static LinearLayout GraphView;
    static GraphViewSeries series;
    static GraphView graphView;
    static  boolean Lock,Stream;
    private static double graph2lastXValue=0;
    private static int Xview=10;
@Override
public void onBackPressed(){
    if(Bluetooth.connectedThread!=null)Bluetooth.connectedThread.write("Q");
    super.onBackPressed();
}


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LinearLayout background =(LinearLayout)findViewById(R.id.bgraph);
        background.setBackgroundColor(Color.BLACK);
        init();
        Buttoninit();
    }
    void init(){
        GraphView=(LinearLayout)findViewById(R.id.bgraph);
        series=new GraphViewSeries("Signal",
                new GraphViewSeries.GraphViewStyle(Color.YELLOW,2),
                new GraphViewData[]{new GraphViewData(0,0)});
        graphView=new LineGraphView(this,"Graph");
        graphView.setLegendAlign(com.jjoe64.graphview.GraphView.LegendAlign.BOTTOM);
        graphView.setShowLegend(true);
        graphView.setScrollable(true);
        graphView.setScalable(true);
        graphView.setManualYAxis(true);
        graphView.setManualYAxisBounds(5,0);
        graphView.addSeries(series);
        GraphView.addView(graphView);


    }
    void Buttoninit(){
        bConnect=(Button)findViewById(R.id.bConnect);
        bConnect.setOnClickListener(this);
        bDisconnect=(Button)findViewById(R.id.bDisconnect);
        bDisconnect.setOnClickListener(this);
        tblock=(ToggleButton)findViewById(R.id.tblock);
        tblock.setOnClickListener(this);
        tbStream=(ToggleButton)findViewById(R.id.tbStream);
        tbStream.setOnClickListener(this);
        Lock =true;
        Stream=false;
    }
    public void onClick(View v){
        switch(v.getId()){
            case R.id.bConnect:
                startActivity(new Intent("android.intent.action.BT"));
                break;
            case R.id.bDisconnect:
                Bluetooth.disconnect();
                break;
            case R.id.tblock:
                if(tblock.isChecked()){
                    Lock=true;
                }else{
                    Lock=false;
                }
                break;
            case R.id.tbStream:
                if(tbStream.isChecked()){
                    if(Bluetooth.connectedThread !=null)Bluetooth.connectedThread.write("E");
                }else{
                    if(Bluetooth.connectedThread !=null)Bluetooth.connectedThread.write("Q");
                }
                break;

        }
    }

}

