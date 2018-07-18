package com.realtimebus;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class BusTimeLayout extends LinearLayout {
	public String seq;
	public ImageView bus;
	public TextView busStop;

	public BusTimeLayout(Context context, String id, String busType, String title, String currentSeq) {
		super(context);
		setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);

		bus = new ImageView(context);
		bus.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
		if (busType == null)
			bus.setBackgroundColor(Color.WHITE);
		else if ("buss".equals(busType))
			bus.setImageResource(R.drawable.buss);
		else if ("busc".equals(busType))
			bus.setImageResource(R.drawable.busc);
		addView(bus);

		busStop = new TextView(context);
		busStop.setPadding(64, 0, 0, 0);
		busStop.setTextSize(20);
		if (title != null) {
			seq = id;
			if (currentSeq.equals(seq))
				busStop.setTextColor(0xff66ccff);
			else if (Integer.parseInt(currentSeq) < Integer.parseInt(seq))
				busStop.setTextColor(Color.LTGRAY);
			else
				busStop.setTextColor(Color.BLACK);
			busStop.setText(title + (!currentSeq.equals(seq) ? "" : " ↻"));
		}
		addView(busStop);
	}
}
