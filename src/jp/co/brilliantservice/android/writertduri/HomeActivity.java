/*
 * Copyright 2012 yamashita@brilliantservice.co.jp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.brilliantservice.android.writertduri;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.Toast;

public class HomeActivity extends Activity {

    public static final String LOG_TAG = HomeActivity.class.getSimpleName();

    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(getApplicationContext(), "not found NFC feature", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "NFC feature is not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()), 0);
        IntentFilter[] intentFilter = new IntentFilter[] {
            new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
        };
        String[][] techList = new String[][] {
                {
                    android.nfc.tech.NdefFormatable.class.getName()
                }, {
                    android.nfc.tech.Ndef.class.getName()
                }
        };
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, techList);

    }

    @Override
    public void onPause() {
        super.onPause();

        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getAction();
        if (TextUtils.isEmpty(action))
            return;

        if (!action.equals(NfcAdapter.ACTION_TECH_DISCOVERED))
            return;

        // NdefMessageを作成
        String uri = "http://www.brilliantservice.co.jp/";
        NdefMessage message = createUriMessage(uri);

        // タグの判定をした後に、書き込みます
        Tag tag = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        List<String> techList = Arrays.asList(tag.getTechList());
        if (techList.contains(Ndef.class.getName())) {
            Ndef ndef = Ndef.get(tag);
            writeNdefToNdefTag(ndef, message);

            Toast.makeText(getApplicationContext(), "wrote NDEF to NDEF tag", Toast.LENGTH_SHORT)
                    .show();
        } else if (techList.contains(NdefFormatable.class.getName())) {
            NdefFormatable ndef = NdefFormatable.get(tag);
            writeNdefToNdefFormatable(ndef, message);

            Toast.makeText(getApplicationContext(), "wrote NDEF to NDEFFormatable tag",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "NDEF Not Supported", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private static List<String> sProtocolList;
    static {
        sProtocolList = new ArrayList<String>();
        sProtocolList.add("");
        sProtocolList.add("http://www.");
        sProtocolList.add("https://www.");
        sProtocolList.add("http://");
        sProtocolList.add("https://");
        sProtocolList.add("tel:");
        sProtocolList.add("mailto:");
        sProtocolList.add("ftp://anonymous:anonymous@");
        sProtocolList.add("ftp://ftp.");
        sProtocolList.add("ftps://");
        sProtocolList.add("sftp://");
        sProtocolList.add("smb://");
        sProtocolList.add("nfs://");
        sProtocolList.add("ftp://");
        sProtocolList.add("dav://");
        sProtocolList.add("news:");
        sProtocolList.add("telnet://");
        sProtocolList.add("imap:");
        sProtocolList.add("rtsp://");
        sProtocolList.add("urn:");
        sProtocolList.add("pop:");
        sProtocolList.add("sip:");
        sProtocolList.add("sips:");
        sProtocolList.add("tftp:");
        sProtocolList.add("btspp://");
        sProtocolList.add("btl2cap://");
        sProtocolList.add("btgoep://");
        sProtocolList.add("tcpobex://");
        sProtocolList.add("irdaobex://");
        sProtocolList.add("file://");
        sProtocolList.add("urn:epc:id:");
        sProtocolList.add("urn:epc:tag:");
        sProtocolList.add("urn:epc:pat:");
        sProtocolList.add("urn:epc:raw:");
        sProtocolList.add("urn:epc:");
        sProtocolList.add("urn:nfc:");
    }

    private int getProtocolIndex(String uri) {
        String protocol;
        for (int i = 1; i < sProtocolList.size(); i++) {
            protocol = sProtocolList.get(i);
            if (uri.startsWith(protocol)) {
                return i;
            }
        }
        return 0;
    }

    private NdefMessage createUriMessage(String uri) {
        try {
            int index = getProtocolIndex(uri);
            String protocol = sProtocolList.get(index);

            String uriBody = uri.replace(protocol, "");
            byte[] uriBodyBytes = uriBody.getBytes("UTF-8");

            ByteArrayBuffer buffer = new ByteArrayBuffer(1 + uriBody.length());
            buffer.append((byte)index);
            buffer.append(uriBodyBytes, 0, uriBodyBytes.length);

            byte[] payload = buffer.toByteArray();
            NdefMessage message = new NdefMessage(new NdefRecord[] {
                new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload)
            });

            return message;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * NdefMessageをNdefタグに書き込みます
     * 
     * @param ndef
     * @param message
     */
    private void writeNdefToNdefTag(Ndef ndef, NdefMessage message) {
        int size = message.toByteArray().length;

        try {
            ndef.connect();
            if (!ndef.isWritable()) {
                Toast.makeText(getApplicationContext(), "Read Only...", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ndef.getMaxSize() < size) {
                Toast.makeText(getApplicationContext(), "Over Size...", Toast.LENGTH_SHORT).show();
                return;
            }

            ndef.writeNdefMessage(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (FormatException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * NdefFormatableタグをNdefにフォーマットし、NdefMessageを書き込みます
     * 
     * @param ndef
     * @param message
     */
    private void writeNdefToNdefFormatable(NdefFormatable ndef, NdefMessage message) {
        try {
            ndef.connect();
            ndef.format(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (FormatException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
