package com.example.rollcall;

public class Student {
	String name;		// ����
	String MAC;			// MAC��ַ
	int id;				// ѧ��
	
	Student(int id,String name,String MAC)
	{
		this.name = new String(name);
		this.MAC = new String(MAC);
		this.id = id;
	}
	
	/*public String toString()
	{
		return ""+id+" "+name+" "+MAC;
	}*/
}

