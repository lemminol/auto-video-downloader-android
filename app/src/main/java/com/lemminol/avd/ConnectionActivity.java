package com.lemminol.avd;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionActivity extends Activity {
    private EndpointManager manager;
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private Switch autoSwitch;
    private EditText ssid, local;
    private TextView currentServer;
    private LinearLayout externalList;
    private final List<String> externals = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        manager = new EndpointManager(this);
        externals.addAll(manager.getExternalEndpoints());
        setContentView(buildUi());
        refreshCurrentServer();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(248,250,252));
        applySystemInsets(root, dp(18), dp(14), dp(18), dp(28));
        LinearLayout header = row(); Button back = button("←"); back.setOnClickListener(v -> finish()); header.addView(back); TextView title = title("연결"); header.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1)); root.addView(header);
        root.addView(label("🔒  현재 서버 주소")); currentServer = valueText("확인 중…"); root.addView(currentServer);

        autoSwitch = new Switch(this); autoSwitch.setText("자동 URL 전환\n지정된 Wi‑Fi에서는 내부망, 그 외에는 외부 엔드포인트를 사용합니다."); autoSwitch.setTextSize(15); autoSwitch.setChecked(manager.isAutoSwitch()); autoSwitch.setPadding(0, dp(18), 0, dp(18)); root.addView(autoSwitch);

        LinearLayout localCard = card(); localCard.addView(section("⌂  서버 주소")); localCard.addView(body("서버 주소 또는 IP를 직접 입력하세요. http:// 또는 https://를 생략하면 HTTPS를 먼저 확인하고, 사설망 주소는 HTTP까지 자동으로 확인합니다."));
        ssid = edit("Wi‑Fi 이름 (선택)", manager.getPreferredSsid()); localCard.addView(ssid);
        local = edit("예: 192.168.1.10:8792 또는 example.com", manager.getLocalEndpoint());
        local.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        localCard.addView(local);
        Button useNetwork = button("현재 네트워크 사용"); useNetwork.setOnClickListener(v -> useCurrentNetwork()); localCard.addView(useNetwork); root.addView(localCard);

        LinearLayout externalCard = card(); externalCard.addView(section("▣  외부 네트워크")); externalCard.addView(body("선호 Wi‑Fi가 아닐 때 위에서부터 연결 가능한 첫 번째 주소를 사용합니다. 등록할 때 HTTPS 또는 HTTP를 선택하세요."));
        externalList = new LinearLayout(this); externalList.setOrientation(LinearLayout.VERTICAL); externalCard.addView(externalList); renderExternalRows();
        Button add = button("＋ 엔드포인트 추가"); add.setOnClickListener(v -> addEndpointDialog()); externalCard.addView(add); root.addView(externalCard);

        Button save = button("저장하고 연결"); save.setOnClickListener(v -> saveSettings()); root.addView(save);
        TextView note = body("주소에 프로토콜을 쓰지 않아도 됩니다. HTTPS를 우선 사용하며, 192.168.x.x / 10.x.x.x / 172.16~31.x.x 같은 사설망 주소는 HTTPS 실패 시 HTTP를 자동으로 확인합니다. Wi‑Fi 이름은 선택 사항입니다."); note.setPadding(0,dp(12),0,0); root.addView(note);
        scroll.addView(root); return scroll;
    }

    private void refreshCurrentServer() {
        currentServer.setText("연결 확인 중…");
        io.execute(() -> { String e = manager.chooseEndpoint(); runOnUiThread(() -> { currentServer.setText(e.isEmpty()?"● 연결 가능한 서버 없음":"● " + e); currentServer.setTextColor(e.isEmpty()?Color.rgb(190,50,50):Color.rgb(43,150,72)); }); });
    }

    private void useCurrentNetwork() {
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        if (!missing.isEmpty()) { requestPermissions(missing.toArray(new String[0]), 2); return; }
        String name = manager.currentSsid();
        if (name.isEmpty()) Toast.makeText(this, "Wi‑Fi 이름을 확인할 수 없습니다. 위치 서비스와 권한을 확인하세요.", Toast.LENGTH_LONG).show();
        else { ssid.setText(name); Toast.makeText(this, "현재 Wi‑Fi를 지정했습니다: " + name, Toast.LENGTH_SHORT).show(); }
    }

    private void renderExternalRows() {
        externalList.removeAllViews();
        if (externals.isEmpty()) { TextView empty = body("등록된 외부 주소가 없습니다."); empty.setPadding(0, dp(8), 0, dp(8)); externalList.addView(empty); return; }
        for (int i=0;i<externals.size();i++) {
            final int index=i; String endpoint=externals.get(i);
            LinearLayout row=row(); row.setPadding(0,dp(5),0,dp(5));
            TextView status=new TextView(this); status.setText("●"); status.setTextSize(17); status.setGravity(Gravity.CENTER); status.setTextColor(Color.GRAY); row.addView(status,new LinearLayout.LayoutParams(dp(22),dp(36)));
            TextView text=endpointText(endpoint); row.addView(text,new LinearLayout.LayoutParams(0,dp(36),1));
            Button up=compactButton("↑", dp(30)); up.setEnabled(i>0); up.setContentDescription("위로 이동"); up.setOnClickListener(v->{String x=externals.remove(index);externals.add(index-1,x);renderExternalRows();}); row.addView(up);
            Button down=compactButton("↓", dp(30)); down.setEnabled(i<externals.size()-1); down.setContentDescription("아래로 이동"); down.setOnClickListener(v->{String x=externals.remove(index);externals.add(index+1,x);renderExternalRows();}); row.addView(down);
            Button del=compactButton("삭제", dp(44)); del.setTextSize(10); del.setContentDescription("엔드포인트 삭제"); del.setOnClickListener(v->{externals.remove(index);renderExternalRows();}); row.addView(del);
            externalList.addView(row);
            io.execute(() -> { boolean ok=manager.probe(endpoint); runOnUiThread(() -> status.setTextColor(ok?Color.rgb(46,170,75):Color.rgb(190,50,50))); });
        }
    }

    private void addEndpointDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(8));
        RadioGroup protocols = new RadioGroup(this);
        protocols.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton https = new RadioButton(this);
        https.setId(View.generateViewId());
        https.setText("https://");
        RadioButton http = new RadioButton(this);
        http.setId(View.generateViewId());
        http.setText("http://");
        protocols.addView(https);
        protocols.addView(http);
        protocols.check(https.getId());
        content.addView(protocols);
        EditText input = edit("example.com:8792 또는 IP:포트", "");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        content.addView(input);
        TextView help = body("HTTP는 연결 내용을 암호화하지 않습니다. HTTPS 서버라면 HTTPS를 선택하세요.");
        content.addView(help);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = s.toString().trim().toLowerCase(java.util.Locale.ROOT);
                if (value.startsWith("https://")) protocols.check(https.getId());
                else if (value.startsWith("http://")) protocols.check(http.getId());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("외부 엔드포인트 추가").setView(content)
                .setNegativeButton("취소", null).setPositiveButton("추가", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            // The selected radio button is authoritative; pasted schemes are not duplicated.
            String address = raw.replaceFirst("(?i)^https?://", "");
            if (address.isEmpty() || address.contains("://")) {
                input.setError("서버 주소를 입력하세요.");
                return;
            }
            String scheme = protocols.getCheckedRadioButtonId() == http.getId() ? "http://" : "https://";
            String endpoint = EndpointManager.normalizeEndpoint(scheme + address, false);
            if (endpoint.isEmpty()) {
                input.setError("서버 주소와 포트를 확인하세요.");
                return;
            }
            if (!externals.contains(endpoint)) externals.add(endpoint);
            renderExternalRows();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void saveSettings() {
        String rawLocal = local.getText().toString().trim();
        List<String> normalized = new ArrayList<>();
        for (String e : externals) {
            String n = EndpointManager.normalizeEndpoint(e, false);
            if (n.isEmpty()) {
                Toast.makeText(this, "외부 HTTP/HTTPS 주소를 확인하세요: " + e, Toast.LENGTH_LONG).show();
                return;
            }
            normalized.add(n);
        }

        if (rawLocal.isEmpty() && normalized.isEmpty()) {
            Toast.makeText(this, "연결할 서버 주소를 입력하세요.", Toast.LENGTH_LONG).show();
            return;
        }

        // If no local/manual URL was entered, save external endpoints only.
        if (rawLocal.isEmpty()) {
            manager.setAutoSwitch(autoSwitch.isChecked());
            manager.setPreferredSsid(ssid.getText().toString().trim());
            manager.setLocalEndpoint("");
            manager.setExternalEndpoints(normalized);
            setResult(RESULT_OK, new Intent());
            finish();
            return;
        }

        Toast.makeText(this, "서버 주소와 프로토콜을 확인 중입니다…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            String resolved = manager.resolveUserEndpoint(rawLocal);
            runOnUiThread(() -> {
                if (resolved.isEmpty()) {
                    StringBuilder tried = new StringBuilder();
                    for (String c : EndpointManager.userEndpointCandidates(rawLocal)) {
                        if (tried.length() > 0) tried.append(" / ");
                        tried.append(c);
                    }
                    Toast.makeText(this, tried.length() == 0
                                    ? "서버 주소 형식을 확인하세요."
                                    : "연결할 수 없습니다. 확인한 주소: " + tried,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                manager.setAutoSwitch(autoSwitch.isChecked());
                manager.setPreferredSsid(ssid.getText().toString().trim());
                manager.setLocalEndpoint(resolved);
                manager.setExternalEndpoints(normalized);
                local.setText(resolved);
                Toast.makeText(this, "연결 주소: " + resolved, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK, new Intent());
                finish();
            });
        });
    }

    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(16),dp(14),dp(16),dp(14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(8),0,dp(8));v.setLayoutParams(lp);GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setStroke(dp(1),Color.rgb(226,232,240));g.setCornerRadius(dp(20));v.setBackground(g);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView title(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(24);t.setTextColor(Color.rgb(40,40,48));t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(12),0,0,0);return t;}
    private TextView section(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(17);t.setTextColor(Color.rgb(48,48,56));t.setPadding(0,dp(4),0,dp(8));return t;}
    private TextView label(String s){TextView t=section(s);t.setPadding(dp(8),dp(12),0,dp(6));return t;}
    private TextView body(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(13);t.setTextColor(Color.rgb(92,92,102));return t;}
    private TextView valueText(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(16);t.setTextColor(Color.rgb(79,70,229));t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(12),0,dp(12),0);GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(248,250,252));g.setStroke(dp(1),Color.rgb(226,232,240));g.setCornerRadius(dp(16));t.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));lp.setMargins(0,0,0,dp(8));t.setLayoutParams(lp);return t;}
    private EditText edit(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(14);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setStroke(dp(1),Color.rgb(190,190,200));g.setCornerRadius(dp(14));e.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52));lp.setMargins(0,dp(5),0,dp(5));e.setLayoutParams(lp);return e;}
    private TextView endpointText(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(12);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);t.setTextColor(Color.rgb(79,70,229));t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(10),0,dp(8),0);GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(248,250,252));g.setStroke(dp(1),Color.rgb(226,232,240));g.setCornerRadius(dp(12));t.setBackground(g);return t;}
    private Button compactButton(String text,int width){Button b=new Button(this);styleButton(b);b.setText(text);b.setTextSize(11);b.setAllCaps(false);b.setMinHeight(0);b.setMinWidth(0);b.setPadding(0,0,0,0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,dp(30));lp.setMargins(dp(2),0,dp(2),0);b.setLayoutParams(lp);return b;}
    private Button button(String text){Button b=new Button(this);styleButton(b);b.setText(text);b.setTextSize(12);b.setMinHeight(0);b.setMinWidth(0);b.setPadding(dp(10),0,dp(10),0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(40));lp.setMargins(dp(4),dp(4),dp(4),dp(4));b.setLayoutParams(lp);return b;}
    private void styleButton(Button button) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(Color.rgb(99,102,241)); shape.setCornerRadius(dp(12));
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF), shape, null));
        button.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xFFCBD5E1,Color.WHITE}));
        button.setAllCaps(false);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void applySystemInsets(View view, int baseLeft, int baseTop, int baseRight, int baseBottom) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(baseLeft + bars.left, baseTop + bars.top, baseRight + bars.right, baseBottom + bars.bottom);
            } else {
                v.setPadding(baseLeft, baseTop, baseRight, baseBottom);
            }
            return insets;
        });
        view.requestApplyInsets();
    }

    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
