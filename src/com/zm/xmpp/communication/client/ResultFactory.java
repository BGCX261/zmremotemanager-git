package com.zm.xmpp.communication.client;

import java.util.HashMap;

import android.content.Context;

import com.zm.epad.core.LogManager;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.result.ResultApp;
import com.zm.xmpp.communication.result.ResultDevice;
import com.zm.xmpp.communication.result.ResultEnv;
import com.zm.xmpp.communication.result.ResultNormal;

public class ResultFactory {
	public static final String TAG = "ClientResultFactory";
	
	public static final int RESULT_NORMAL = 1;
	public static final int RESULT_APP = 2;
	public static final int RESULT_DEVICE = 3;
	public static final int RESULT_ENV = 4;
	
	private Context mContext = null;
	private HashMap<String, ResultCallback> mCbMap = new HashMap<String, ResultCallback>();
	
	public ResultFactory(Context context)
	{
		mContext = context;
	}
	
	public interface ResultCallback{
		public void handleResult(IResult result);
	}
	
	public IResult getResult(int type, String id, String status, ResultCallback callback)
	{
		IResult ret = null;
		
		switch(type){
		case RESULT_NORMAL:
			ret = new ResultNormal();
			break;
		case RESULT_APP:
			ret = ConfigResultApp(new ResultApp());
			break;
		case RESULT_DEVICE:
			ret = ConfigResultDevice(new ResultDevice());
			break;
		case RESULT_ENV:
			ret = ConfigResultEnv(new ResultEnv());
			break;
		default:
			LogManager.local(TAG, "bad type: " + type);
			ret = null;
			break;
		}

		if(ret!=null)
		{
			ret.setId(id);
			ret.setStatus(status);
		}			

		return ret;
	}
	
	public IResult getResult(int type, String id)
	{
		return getResult(type, id, null, null);
	}
	
	public IResult getResult(int type, String id, String status)
	{
		return getResult(type, id, status, null);
	}
	
	private IResult ConfigResultApp(ResultApp result)
	{
		return result;
	}
	
	private IResult ConfigResultDevice(ResultDevice result)
	{
		return result;
	}
	
	private IResult ConfigResultEnv(ResultEnv result)
	{
		return result;
	}
	
	private void addCallback(String id, ResultCallback callback)
	{
		mCbMap.put(id, callback);
	}
	
	private void sendCallback(String id, IResult result)
	{
		ResultCallback cb = mCbMap.get(id);
		cb.handleResult(result);
		mCbMap.remove(id);
	}
}
