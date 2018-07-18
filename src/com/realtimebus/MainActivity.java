package com.realtimebus;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnTouchListener, View.OnClickListener {

	private Button searchBtn;
	private EditText busNumberEdit;
	private TextView lineDirTV, tipsTV, reverseBtn;
	private ScrollView busTimeSV;
	private LinearLayout busTimeSL;
	private RelativeLayout lineDirBar;
	private String m_busNumber;
	private List<LineDir> m_lineDirs;
	private int m_currentLineDir;
	private List<BusTime> m_busTimes;
	private String m_currentSeq;
	private String m_tips;
	private MyHandler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// ���ر�����
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			setTranslucentStatus(true);
			StatusBarCompat sbCompat = new StatusBarCompat(this);
			sbCompat.setStatusBarTintEnabled(true);
			sbCompat.setStatusBarTintResource(R.color.LuoTianyi);// ֪ͨ��������ɫ
		}
		setContentView(R.layout.activity_main);
		searchBtn = (Button) findViewById(R.id.searchBtn);
		reverseBtn = (TextView) findViewById(R.id.reverseBtn);
		busNumberEdit = (EditText) findViewById(R.id.busNumEdit);
		lineDirTV = (TextView) findViewById(R.id.lineDirTV);
		tipsTV = (TextView) findViewById(R.id.tipsTV);
		busTimeSV = (ScrollView) findViewById(R.id.busTimeSV);
		busTimeSL = (LinearLayout) findViewById(R.id.busTimeSL);
		lineDirBar = (RelativeLayout) findViewById(R.id.lineDirBar);
		handler = new MyHandler();
		searchBtn.setOnClickListener(this);
		reverseBtn.setOnClickListener(this);
		busNumberEdit.setOnTouchListener(this);
		busNumberEdit.setRawInputType(Configuration.KEYBOARD_QWERTY);// Ĭ�����ּ���
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == busNumberEdit && MotionEvent.ACTION_DOWN == event.getAction()) {
			busNumberEdit.setCursorVisible(true);// �ٴε����ʾ���
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		if (v == searchBtn) {
			busNumberEdit.clearFocus();// ȡ������
			busNumberEdit.setCursorVisible(false);// ������أ������û��������
			((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
					this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);// �ر����뷨
			m_busNumber = busNumberEdit.getText().toString();
			lineDirBar.setVisibility(View.INVISIBLE);
			tipsTV.setText(m_tips = "");
			busTimeSL.removeAllViews();
			new GetLineDirThread(m_busNumber).start();// ��get��������
		} else if (v == reverseBtn) {
			if (m_lineDirs == null || m_lineDirs.size() < 2)
				return;
			m_currentLineDir = 1 - m_currentLineDir;
			lineDirTV.setText(m_lineDirs.get(m_currentLineDir).m_text);
			busTimeSV.fullScroll(ScrollView.FOCUS_UP);
			new GetBusTimeThread(m_busNumber, m_lineDirs.get(m_currentLineDir).m_value, "1").start();// ��get��������
		}
	}

	@TargetApi(19)
	private void setTranslucentStatus(boolean on) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
	}

	// �����̣߳���Ϊ���������̷߳���Intent
	class GetBusTimeThread extends Thread {
		private String m_selBLine;
		private String m_selBDir;
		private String m_selBStop;

		public GetBusTimeThread(String selBLine, String selBDir, String selBStop) {
			super();
			m_selBLine = selBLine;
			m_selBDir = selBDir;
			m_selBStop = selBStop;
		}

		public void run() {
			HttpURLConnection conn = null;// �������Ӷ���
			String urlStr = "http://www.bjbus.com/home/ajax_rtbus_data.php?act=busTime" + "&selBLine=" + m_selBLine
					+ "&selBDir=" + m_selBDir + "&selBStop=" + m_selBStop;
			try {
				URL url = new URL(urlStr); // URL����
				conn = (HttpURLConnection) url.openConnection(); // ʹ��URL��һ������,���������������
				conn.setRequestMethod("GET"); // ʹ��get����

				if (conn.getResponseCode() == 200) {// ����200��ʾ���ӳɹ�
					InputStream is = conn.getInputStream(); // ��ȡ������
					getBusTimes(is);
					// �����̷߳�����Ϣ
					Bundle bundle = new Bundle();
					bundle.putString("from", "BusTimeThread");// bundle��Ҳ���Է����л�������������������
					Message msg = handler.obtainMessage();// ÿ����һ�ζ�Ҫ���»�ȡ
					msg.setData(bundle);
					handler.sendMessage(msg);// ��handler�����̷߳�����Ϣ
				}
			} catch (IOException e) {
				m_tips = "��������ʧ�ܣ������ԣ��������ڻ�ȡ����վ������ʱ��������δ�����������ӣ�\n" + e.toString();
				// �����̷߳�����Ϣ
				Bundle bundle = new Bundle();
				bundle.putString("from", "Error");// bundle��Ҳ���Է����л�������������������
				Message msg = handler.obtainMessage();// ÿ����һ�ζ�Ҫ���»�ȡ
				msg.setData(bundle);
				handler.sendMessage(msg);// ��handler�����̷߳�����Ϣ
			}
		}
	}

	// �����̣߳���Ϊ���������̷߳���Intent
	class GetLineDirThread extends Thread {
		private String m_selBLine;

		public GetLineDirThread(String selBLine) {
			super();
			m_selBLine = selBLine;
		}

		public void run() {
			HttpURLConnection conn = null;// �������Ӷ���
			String urlStr = "http://www.bjbus.com/home/ajax_rtbus_data.php?act=getLineDirOption" + "&selBLine="
					+ m_selBLine;
			try {
				URL url = new URL(urlStr); // URL����
				conn = (HttpURLConnection) url.openConnection(); // ʹ��URL��һ������,���������������
				conn.setRequestMethod("GET"); // ʹ��get����

				if (conn.getResponseCode() == 200) {// ����200��ʾ���ӳɹ�
					InputStream is = conn.getInputStream(); // ��ȡ������
					getLineDirs(is);
					// �����̷߳�����Ϣ
					Bundle bundle = new Bundle();
					bundle.putString("from", "LineDirThread");// bundle��Ҳ���Է����л�������������������
					Message msg = handler.obtainMessage();// ÿ����һ�ζ�Ҫ���»�ȡ
					msg.setData(bundle);
					handler.sendMessage(msg);// ��handler�����̷߳�����Ϣ
				}
			} catch (IOException e) {
				m_tips = "��������ʧ�ܣ������ԣ��������ڻ�ȡ�������з���ʱ��������δ�����������ӣ�\n" + e.toString();
				// �����̷߳�����Ϣ
				Bundle bundle = new Bundle();
				bundle.putString("from", "Error");// bundle��Ҳ���Է����л�������������������
				Message msg = handler.obtainMessage();// ÿ����һ�ζ�Ҫ���»�ȡ
				msg.setData(bundle);
				handler.sendMessage(msg);// ��handler�����̷߳�����Ϣ
			}
		}
	}

	// �Զ���handler��
	@SuppressLint("HandlerLeak")
	class MyHandler extends Handler {
		// ���ձ���̵߳���Ϣ������
		@Override
		public void handleMessage(Message msg) {
			String from = msg.getData().get("from").toString();
			if ("LineDirThread".equals(from)) {
				if (m_lineDirs == null || m_lineDirs.isEmpty()) {
					m_tips = "δ�ܻ�ȡ" + m_busNumber + "·��������Ϣ�������ԣ������������˴���Ĺ������룬�����粻�ȶ���\n" + m_tips;
					tipsTV.setText(m_tips);
					busTimeSL.removeAllViews();
					return;
				}
				m_currentLineDir = 0;
				lineDirTV.setText(m_lineDirs.get(m_currentLineDir).m_text);
				lineDirBar.setVisibility(View.VISIBLE);
				new GetBusTimeThread(m_busNumber, m_lineDirs.get(m_currentLineDir).m_value, "1").start();// ��get��������
			} else if ("BusTimeThread".equals(from)) {
				if (m_busTimes == null || m_busTimes.isEmpty()) {
					m_tips = "δ�ܻ�ȡ" + m_busNumber + "·������վ����Ϣ�������ԣ������Ǹ÷�����ֹͣ��Ӫ�������粻�ȶ���\n" + m_tips;
					tipsTV.setText(m_tips);
					busTimeSL.removeAllViews();
					return;
				}
				int busCount = 0;
				for (BusTime bustime : m_busTimes)
					if (bustime.m_busType != null)
						++busCount;
				if (busCount == 0)
					m_tips += "�������δ֪\n";
				else {
					int busCircle = (m_busTimes.size() * 3) / (busCount * 2);
					m_tips += "�������Լ" + busCircle + "����\n";
				}
				tipsTV.setText(m_tips);
				busTimeSL.removeAllViews();
				for (BusTime bt : m_busTimes) {
					BusTimeLayout layout = new BusTimeLayout(getApplicationContext(), bt.m_id, bt.m_busType, bt.m_title,
							m_currentSeq);
					busTimeSL.addView(layout);
					layout.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							BusTimeLayout layout = (BusTimeLayout) v;
							if (layout.seq != null)
								new GetBusTimeThread(m_busNumber, m_lineDirs.get(m_currentLineDir).m_value, layout.seq)
										.start();// ��get��������
						}
					});
				}
			} else if ("Error".equals(from)) {
				tipsTV.setText(m_tips);
				busTimeSL.removeAllViews();
			}
		}
	}

	// �������������ص�xml��Ϣ
	public void getLineDirs(InputStream is) {
		m_lineDirs = new ArrayList<LineDir>();
		try {
			// ��ȡXmlPullParser����
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(is, "utf-8");
			// ��ȡָ��
			int type = parser.getEventType();
			// type�����ĵ�����
			while (type != XmlPullParser.END_DOCUMENT) {
				switch (type) {
				case XmlPullParser.START_TAG: // ��ʼ��ǩ
					// �õ���ǩ�����ж�
					if ("option".equals(parser.getName())) {
						String value = parser.getAttributeValue(null, "value");
						if ("".equals(value))
							break;
						// ��ȡ��������ǰָ��Ԫ�ص���һ���ı��ڵ��ֵ
						String text = parser.nextText();
						text = text.substring(text.indexOf('(') + 1, text.indexOf(')'));
						m_lineDirs.add(new LineDir(value, text));
					}
					break;
				case XmlPullParser.END_TAG: // ������ǩ
					break;
				}
				type = parser.next();
			}
		} catch (Exception e) {
			m_tips = "XML���ݽ������̳��������ԣ����������粻�ȶ���\n" + e.toString();
		}
	}

	// �������������ص�json��Ϣ
	public void getBusTimes(InputStream is) {
		m_tips = "";
		m_busTimes = new ArrayList<BusTime>();
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader bufferReader = new BufferedReader(isr);
			String jsonString = "";
			String inputLine = "";
			while ((inputLine = bufferReader.readLine()) != null) {
				jsonString += inputLine + "\n";
			}
			JSONObject json = new JSONObject(jsonString);
			String html = json.getString("html");
			json.getString("w");
			m_currentSeq = json.getString("seq");
			// ��ʼ����HTML
			html = html.replace("&nbsp;", " ");
			boolean flag1 = false;
			boolean flag2 = false;
			BusTime busTime = null;
			// ��ȡXmlPullParser����
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(new ByteArrayInputStream(html.getBytes("UTF-8")), "utf-8");
			// ��ȡָ��
			int type = parser.getEventType();
			// type�����ĵ�����
			while (type != XmlPullParser.END_DOCUMENT) {
				switch (type) {
				case XmlPullParser.START_TAG: // ��ʼ��ǩ
					// �õ���ǩ�����ж�
					if ("article".equals(parser.getName())) {
						flag1 = true;
					} else if ("li".equals(parser.getName())) {
						flag2 = true;
						busTime = new BusTime();
					} else if ("div".equals(parser.getName())) {
						if (flag2)
							busTime.m_id = parser.getAttributeValue(null, "id");
					} else if ("i".equals(parser.getName())) {
						if (flag2)
							busTime.m_busType = parser.getAttributeValue(null, "class");
					} else if ("span".equals(parser.getName())) {
						if (flag2)
							busTime.m_title = parser.getAttributeValue(null, "title");
					}
					break;
				case XmlPullParser.END_TAG: // ������ǩ
					if ("article".equals(parser.getName())) {
						flag1 = false;
					} else if ("p".equals(parser.getName())) {
						if (flag1)
							m_tips += "\n";
					} else if ("li".equals(parser.getName())) {
						flag2 = false;
						m_busTimes.add(busTime);
						busTime = null;
					}
					break;
				case XmlPullParser.TEXT:
					if (flag1)
						m_tips += parser.getText();
					break;
				}
				type = parser.next();
			}
		} catch (Exception e) {
			m_tips = "JSON���ݽ������̳��������ԣ����������粻�ȶ���\n" + e.toString();
		}
	}
}
