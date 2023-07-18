// IaidlSnowInterface.aidl
package cn.snow.interviewapp.aidl;

import cn.snow.interviewapp.aidl.IPCTestBean;

// Declare any non-default types here with import statements
//AIDL支持的类型：八大基本数据类型、String类型、CharSequence、List、Map、自定义类型。List、Map、自定义类型
interface IaidlSnowInterface {

    int getProcessId();

    String getProcessName();

    IPCTestBean getIPCTestBean();

    void setIPCTestBean(inout IPCTestBean bean);
}