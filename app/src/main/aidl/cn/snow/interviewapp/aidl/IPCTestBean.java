package cn.snow.interviewapp.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class IPCTestBean implements Parcelable {

    public IPCTestBean() {
    }

    private String name;
    private int age;

    public IPCTestBean(String name, int age) {
        this.name = name;
        this.age = age;
    }

    protected IPCTestBean(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<IPCTestBean> CREATOR = new Creator<IPCTestBean>() {
        @Override
        public IPCTestBean createFromParcel(Parcel in) {
            return new IPCTestBean(in);
        }

        @Override
        public IPCTestBean[] newArray(int size) {
            return new IPCTestBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(age);
    }


    //在aidl文件中使用out或者inout修饰时需要自己添加这个方法
    public void readFromParcel(Parcel dest) {
        name = dest.readString();
        age = dest.readInt();
    }

    @Override
    public String toString() {
        return "IPCTestBean{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
