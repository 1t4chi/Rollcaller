package com.example.rollcall;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	// �������
	Spinner spChooseClass;
	ListView list;
	TextView textInfo;		// ��ʾ��Ϣ�õ��ı���
	Button btnStart;
	Button btnSave;
	Button btnAdd;
	View.OnClickListener btnStartListener;
	View.OnClickListener btnSaveListener;
	View.OnClickListener btnAddListener;
	
	//AlertDialog deleteDialog;
	
	ArrayAdapter<String> adapterForInClass;// = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);		// ��ʼ����ǰ��ʾѧ����Ϣ��������
	ArrayAdapter<String> adapterForRollCall;// =new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);		// ����ʱ�͵�����������ʾ�����������
	ArrayAdapter<String> adapterClasses;	// ѡ��༶��spinner��������
	
	// �ļ��洢���
	File sdCard;		//Environment.getExternalStorageDirectory();
	File pathInfo; //= new File(sdCard,"/RollCall/Info");
	File pathRecord; //= new File(sdCard,"/RollCall/Record");
	String calssFileName;					// �༶�ļ���
	BufferedReader reader;
	BufferedWriter writer;
	
	// ���������
	String classNameOfFileName;
	String result;
	boolean hasResult = false;
	
	// ������ļ���ȡ����ѧ����Ϣ
	ArrayList<Student> allStudentsInClass = new ArrayList<>();		// ��������ѧ��
	ArrayList<Student> attendantStudents = new ArrayList<>();		// ����ɨ�赽��ѧ��
	
	// ��־����
	boolean rollCalling = false;
	boolean doneOnceRollCall = false;			// ������һ�ε������ڵ����������������ť����Ϊtrue
	boolean unregistered = true;					// ��ǹ㲥�������Ƿ��Ѿ�ע��
	boolean timerCanceled = true;				// ��Ƕ�ʱ���Ƿ�ȡ��
	
	// WLAN���
	WifiManager wifi;
	BroadcastReceiver wifiResultReceiver;
	Timer wifiScanTimer;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        spChooseClass = (Spinner) findViewById(R.id.spinnerChooseClass);
        list = (ListView) findViewById(R.id.listViewStudents);
        textInfo = (TextView) findViewById(R.id.textInfo);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnAdd = (Button) findViewById(R.id.btnAddStudent);
        
        textInfo.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        
       // textInfo.setTypeface(getResources().BOLD); 
        
        adapterForRollCall = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);		// ����ʱ�͵�����������ʾ�����������
        
        wifiScanTimer = new Timer(true);
        
        //btnSave.setClickable(false);		// һ��ʼ�����¼��ť�ǲ��ܰ���
        //btnSave.setAlpha(0.5f);
        
        sdCard = Environment.getExternalStorageDirectory();
        pathInfo = new File(sdCard,"/RollCall/Info");
    	pathRecord = new File(sdCard,"/RollCall/Record");
        if(!pathInfo.exists())		// û��Info�ļ����򴴽��ļ���
        {
        	pathInfo.mkdirs();
        }
        if(!pathRecord.exists())	// û��Record�ļ����򴴽��ļ���
        {
        	pathRecord.mkdirs();
        }
        
        
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);		// ���wifi������
        wifiResultReceiver = new BroadcastReceiver() {						// ɨ�赽һ���µ�WLAN�ڵ�ļ���
			@Override
			public void onReceive(Context context, Intent intent) {
				List<ScanResult> scanResult = wifi.getScanResults();
				addToAttendantStudents(scanResult,allStudentsInClass,attendantStudents,adapterForRollCall);		// ���µ�ɨ�������ݹ�����ӵ�����ѧ��������,����������������
				refreshDisplay();							// ˢ����ʾ�������и�����������������ˢ����Ϣ��ʾ�ı���
			}
		};
        
		// ���ѧ���ļ�¼
		btnAddListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				LayoutInflater inflater = MainActivity.this.getLayoutInflater();
				
				final View dialogView = inflater.inflate(R.layout.dialog_add_student, null);
				
				((EditText) dialogView.findViewById(R.id.className)).setText((String) spChooseClass.getSelectedItem());		// Ĭ���ǵ�ǰѡ��İ༶
				builder.setView(dialogView)
				.setPositiveButton("����", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String className = ((EditText) dialogView.findViewById(R.id.className)).getText().toString();
						String sID = ((EditText) dialogView.findViewById(R.id.id)).getText().toString();
						String name = ((EditText) dialogView.findViewById(R.id.name)).getText().toString();
						String MAC = ((EditText) dialogView.findViewById(R.id.MAC)).getText().toString();
						if((className==null||sID==null||name==null||MAC==null)||
								(className.length()==0
								||sID.length()==0
								||name.length()==0
								||MAC.length()==0))
						{
							Toast.makeText(MainActivity.this,"�������Ϣ", Toast.LENGTH_SHORT).show();
							return;
						}
					
						boolean isNum = true;
						for(int i=0;i<sID.length();i++)
						{
							if((sID.charAt(i)-'0')<0||(sID.charAt(i)-'0')>9)
							{
								isNum = false;
								break;
							}
						}
						if(!isNum)
						{
							Toast.makeText(MainActivity.this,"ѧ�ű���Ϊ����", Toast.LENGTH_SHORT).show();
							return;
						}
						
						addStudent(className+".txt",Integer.parseInt(sID),name,MAC);
					}
				})
				.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.show();
			}
		};
		btnAdd.setOnClickListener(btnAddListener);
        
        // ��ʼ������ť����Ӧ
        btnStartListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(rollCalling)		// ���ڵ���
				{
					rollCalling = false;
					wifiScanTimer.purge();
					timerCanceled = true;
					unregisterReceiver(wifiResultReceiver);		// ע��ɨ�赽wlan�����ļ���
					unregistered = true;
					//btnSave.setClickable(true);		// һ��ʼ�����¼��ť�ǲ��ܰ���
			        //btnSave.setAlpha(1.0f);
					hasResult = true;
					
					result = getResult(allStudentsInClass,attendantStudents);	// ͳ�Ƶ��������������ʾ���ı���ؼ���
					textInfo.setText(result);
					btnStart.setText("��ʼ����");
					
					//spChooseClass.setClickable(true);			// ����ѡ��༶��
					spChooseClass.setEnabled(true);			// ����ѡ��༶��
					btnAdd.setClickable(true);
				}
				else				// ��û��ʼ����
				{
					if(calssFileName!=null)
					{
						if(wifi.isWifiEnabled())
						{
							rollCalling = true;
							registerReceiver(wifiResultReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));		// ע����ɨ�����Ĺ㲥����
							
							//�����ڿ�ʼwifiɨ��
							wifiScanTimer.schedule(new TimerTask() {
								@Override
								public void run() {
									wifi.startScan();
								}
							}, 500, 10000);
							timerCanceled = false;
							hasResult = false;
							
							
							classNameOfFileName = new String(calssFileName);
							unregistered = false;
							
							//spChooseClass.setClickable(false);			// ����ѡ��༶��
							spChooseClass.setEnabled(false);			// ����ѡ��༶��
							btnAdd.setClickable(false);
							
							
							attendantStudents.clear();					// �����һ�ε����Ľ��
							adapterForRollCall.clear();
							list.setAdapter(adapterForRollCall);		// �л�����������
							
							textInfo.setText("���ڵ���...");
							btnStart.setText("ֹͣ����");
						}
						else
						{
							Toast.makeText(MainActivity.this, "���ȿ���wifi", Toast.LENGTH_SHORT).show();
						}
					}
					else
					{
						Toast.makeText(MainActivity.this, "�༶Ϊ�գ��޷�������", Toast.LENGTH_SHORT).show();
					}
				}
			}
		};
        btnStart.setOnClickListener(btnStartListener);
        
        // �����¼��ť����Ӧ
        btnSaveListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(hasResult)
				{
					saveResult();
					Toast.makeText(MainActivity.this,"����ѱ���",Toast.LENGTH_SHORT).show();
					hasResult = false;
				}
				else
				{
					Toast.makeText(MainActivity.this, "��û�е�����������ȵ���", Toast.LENGTH_SHORT).show();
				}
					//btnSave.setClickable(false);		// һ��ʼ�����¼��ť�ǲ��ܰ���
			        //btnSave.setAlpha(0.5f);
			}
		};
        btnSave.setOnClickListener(btnSaveListener);
        
       // �б�ؼ�ListView��ĳ��ѡ�����ʱ����Ӧ
        list.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if(!rollCalling)		// ���ڵ�����û����Ӧ
				{
					String temp = (String) parent.getItemAtPosition(position);		// ��ȡ����е��ı�����
					String[] a = temp.split("\t");
					final int studentId = Integer.parseInt(a[0]);			// ��ȡѧ��

					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);	// �Ի�������
					builder.setTitle("��ʾ");
					builder.setMessage("ȷ��ɾ���ü�¼��");
					builder.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							deleteStudentFromAdapterAndArraylistForIncalss(pathInfo,calssFileName+".txt",allStudentsInClass,adapterForInClass,studentId);	// ɾ����¼
						}
					});
					builder.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
					builder.show();
					
				}
				return true;
				//return false;
			}
        	
		});
        
        
        // ѡ��༶����Ӧ
        spChooseClass.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,int position, long id) {
				calssFileName = adapterClasses.getItem(position);		// ���ѡ��İ༶
				adapterForInClass = makeAdapterAndArrayListForInclass(pathInfo,calssFileName+".txt",allStudentsInClass);
				list.setAdapter(adapterForInClass);	
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
		});
    }
    
    @Override
    protected void onStart()
    {
    	super.onStart();

    	updateClassSpinner();
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	if(!unregistered && !rollCalling)	// û�ڵ�����ûע���㲥��������Ӧ�ùر�
    	{
    		unregisterReceiver(wifiResultReceiver);
    		
    	}
    	if(!timerCanceled&&!rollCalling)
    	{
    		wifiScanTimer.cancel();
    	}
    }
    
    @Override
    protected void onStop()
    {
    	super.onStop();
    }
    
    
    /**
	 * �÷������ļ��ж�ȡѧ����Ϣ�������ڴ����ArrayList<Student>�У�������������������Ϊ����ֵ����
	 */
    private ArrayAdapter<String> makeAdapterAndArrayListForInclass(File path,String calssFileName,ArrayList<Student> allStudentsInClass)
    {
    	File toRead = new File(path,calssFileName);
    	try {
			reader = new BufferedReader(new FileReader(toRead));
		
			allStudentsInClass.clear();
			String line;
			while(true)	//line=reader.readLine())!=null
			{
				line=reader.readLine();
				if(line==null)
					break;
				String[] info = line.split(" ");
				int id = Integer.parseInt(info[0]);		// ѧ��
				String name = info[1];
				String MAC = info[2];
				allStudentsInClass.add(new Student(id,name,MAC));
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	Comparator<Student> comparator = new Comparator<Student>() {

			@Override
			public int compare(Student arg0, Student arg1) {

				if(arg0.id>arg1.id)
					return 1;
				if(arg0.id==arg1.id)
					return 0;
				else 
					return -1;
			}
    	};
     Collections.sort(allStudentsInClass, comparator);
    	
    	
    	
    	ArrayAdapter<String> adp = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    	for(int i=0;i<allStudentsInClass.size();i++)
    	{
    		String s = ""+allStudentsInClass.get(i).id+"\t"+allStudentsInClass.get(i).name;
    		adp.add(s);
    	}
    	return adp;
    }
    
    /**
     * �÷�����������������һ����Ӧ�ó�ϯ��ѧ���ļ��ϣ�һ�����Ѿ���ϯ��ѧ���ļ��ϣ������ַ������ͳ�ƽ��
     * @param studentsShouldAttendant Ӧ�ó�ϯ��ѧ���ļ���
     * @param studentsAttendant �Ѿ���ϯ��ѧ���ļ���
     * @return ���ͳ�ƽ�����ַ���
     */
    private String getResult(ArrayList<Student> studentsShouldAttendant,ArrayList<Student> studentsAttendant)
    {
    	String result = "δ��ѧ��:\n";
    	
    	for(int i=0;i<studentsShouldAttendant.size();i++)
    	{
    		boolean found = false;
    		for(int j=0;j<studentsAttendant.size();j++)
    		{
    			if(studentsShouldAttendant.get(i).id==studentsAttendant.get(j).id)
    			{
    				found = true;
    				break;
    			}
    		}
    		if(!found)
    		{
    			result += studentsShouldAttendant.get(i).name+"\n";
    		}
    	}
    	
    	return result;
    }
    
    /**
     * �÷�������wifiɨ�����������ڸð��ѧ������ɨ������MAC��ַ���ڸð༶����ӵ�����ѧ�������У�����������listview��ʾ��������������ˢ����ʾ
     * @param scanResult wifiɨ����
     * @param allStudentsInClass �ð༶������ѧ���ļ���
     * @param attendantStudents ���õ�ѧ���ļ���
     * @param adapterForRollCall ���ڵ���ʱʹ�õ�������
     */
    private void addToAttendantStudents(List<ScanResult> scanResults,ArrayList<Student> allStudentsInClass,ArrayList<Student> attendantStudents,ArrayAdapter<String> adapterForRollCall)
    {
    	for(ScanResult ap:scanResults)
    	{
    		boolean alreadyExist = false;
    		for(Student st:attendantStudents)
    		{
    			if(st.MAC.toUpperCase().equals(ap.BSSID.toUpperCase()))
    			{
    				alreadyExist = true;
    				break;
    			}
    		}
    		if(alreadyExist)
    			break;
    		
    		for(int i=0;i<allStudentsInClass.size();i++)
    		{
    			if(allStudentsInClass.get(i).MAC.toUpperCase().equals(ap.BSSID.toUpperCase()))
    			{
    				attendantStudents.add(allStudentsInClass.get(i));
    				adapterForRollCall.add(allStudentsInClass.get(i).id+"\t"+allStudentsInClass.get(i).name+"\t	��");
    			}
    		}
    	}
    }
    
    /**
     * �÷���ˢ����ʾ��������1��ˢ��listview��ʾ��2��ˢ���ı���
     */
    private void refreshDisplay()
    {
    	adapterForRollCall.notifyDataSetChanged();		// ֪ͨ���ݼ��ı�
    	textInfo.setText("Ӧ�������� "+allStudentsInClass.size()+"\n"+"�ѵ�������"+attendantStudents.size());
    }
    
    /**
     * ����ѧ����ѧ�ţ����������������ļ���ɾ����Ӧѧ���ļ�¼
     * @param path �ļ�·��
     * @param calssFileName �ļ���
     * @param allStudentsInClass ��������ѧ������
     * @param adapterForInClass ����������
     * @param studentId ѧ��
     */
    private void deleteStudentFromAdapterAndArraylistForIncalss(
    		File path,
    		String calssFileName,
    		ArrayList<Student> allStudentsInClass,
    		ArrayAdapter<String> adapterForInClass,
    		int studentId)
    {
    	// �����ݼ���ɾ��ѧ��
    	int i=0;
    	boolean found = false;
    	for(i=0;i<allStudentsInClass.size();i++)
    	{
    		if(allStudentsInClass.get(i).id==studentId)
    		{
    			found = true;
    			break;
    		}
    	}
    	if(found)
    		allStudentsInClass.remove(i);
    	
    	// ����������������������ѧ��
    	adapterForInClass.clear();
    	for(i=0;i<allStudentsInClass.size();i++)
    	{
    		String s = ""+allStudentsInClass.get(i).id+"\t"+allStudentsInClass.get(i).name;
    		adapterForInClass.add(s);
    	}
    	adapterForInClass.notifyDataSetChanged();
    	
    	// ���ļ���ɾ��ѧ��
    	File file = new File(path,calssFileName);
    	file.delete();
    	file = new File(path,calssFileName);
    	try {
			writer = new BufferedWriter(new FileWriter(file));
			for(Student st:allStudentsInClass)
			{
				writer.write(st.id+" "+st.name+" "+st.MAC+"\n");
			}
			writer.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }
    
    private void addStudent(String className,int id,String studentName,String MAC)
    {
    	
    	// pathInfo-ѧ����Ϣ�ļ��ı���Ŀ¼
    	
    	if(className.equals(calssFileName+".txt"))
    	{ 
    		// ������ӵ��ǵ�ǰѡ�а༶��ѧ������ô��Ӧ��Ҫ�Ѹ�ѧ����ӵ�ѧ��������
        	Student studentToAdd = new Student(id, studentName, MAC);
        	allStudentsInClass.add(studentToAdd);
        	
        	// ��Ӧ������������ҲҪ�ı�
        	adapterForInClass.add(studentToAdd.id+"\t"+studentToAdd.name+"\t");
        	adapterForInClass.notifyDataSetChanged();
    	}
    	
    	
    	// ���ļ�������ѧ����¼
    	File file = new File(pathInfo,className);
    	try {
			writer = new BufferedWriter(new FileWriter(file,true));
			writer.append(""+id+" "+studentName+" "+MAC+"\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	if(!className.equals(calssFileName+".txt"))
    	{
    		updateClassSpinner();
    	}
    }
    
    	
    	
    	/*δʵ�ֵ�ȥ�ع���
    	
    	// pathInfo-ѧ����Ϣ�ļ��ı���Ŀ¼
    	// ������ӵ��ǵ�ǰѡ�а༶��ѧ������ô��Ӧ��Ҫ�Ѹ�ѧ����ӵ�ѧ��������
    	if(className.equals(calssFileName+".txt"))
    	{ 
    		
        	boolean exist = false;
    		for(int i=0;i<allStudentsInClass.size();i++)
        	{
        		if(allStudentsInClass.get(i).id==id)
        		{
        			allStudentsInClass.get(i).name = studentName;
        			allStudentsInClass.get(i).MAC = MAC;
        			exist = true;
        			break;
        		}
        		
        	}
    		if(!exist)
    		{
    			Student studentToAdd = new Student(id, studentName, MAC);
            	allStudentsInClass.add(studentToAdd);
    		}
    		
        	
        	
        	//��Ӧ������������ҲҪ�ı�
        	adapterForInClass.add(studentToAdd.id+"\t"+studentToAdd.name+"\t");
    		adapterForInClass.clear();
    		adapterForInClass.addAll(allStudentsInClass);
        	adapterForInClass.notifyDataSetChanged();
    	}
    	
    	
    	// ���ļ�������ѧ����¼
    	File file = new File(pathInfo,className);
    	try {
			writer = new BufferedWriter(new FileWriter(file,true));
			writer.append(""+id+" "+studentName+" "+MAC+"\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	if(!className.equals(calssFileName+".txt"))
    	{
    		updateClassSpinner();
    	}
    	*/
    
    
    private void saveResult()
    {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    	String s = sdf.format(new Date());
    	File saveFile = new File(pathRecord, s+"_"+classNameOfFileName+".txt");
    	try {
			writer = new BufferedWriter(new FileWriter(saveFile));
			writer.write(result);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * ���°༶�����б�ķ���
     */
    private void updateClassSpinner()
    {
    	String[] classes = pathInfo.list();		// ��ȡ�����ļ����ļ���
    	if(classes!=null&&classes.length>0)
    	{
    		for(int i=0;i<classes.length;i++)
        	{
    			if(classes[i].length() >4)
        		classes[i]=classes[i].substring(0,classes[i].length()-4);
        	}
    		adapterClasses = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, classes);
    		spChooseClass.setAdapter(adapterClasses);		// ���༶�ļ�����Ϊѡ��
    		
    		
    		int size = spChooseClass.getCount();
    		for(int i=0;i<size;i++)
    		{
    			if(((String)spChooseClass.getItemAtPosition(i)).equals(calssFileName))
    			{
    				spChooseClass.setSelection(i);
    				break;
    			}
    		}
    		
    	}
    }
    
}
