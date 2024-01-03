package net.xdow.aliyundrive.net;

import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.exception.NotAuthenticatedException;
import net.xdow.aliyundrive.util.JsonUtils;
import net.xdow.aliyundrive.util.TypeReference;
import okhttp3.Call;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public class AliyunDriveCall<T extends AliyunDriveResponse.GenericMessageInfo> extends TypeReference<T> {

    private final Call mCall;
    private final Class<? extends AliyunDriveResponse.GenericMessageInfo> mEntityBeanType;

    private T mMockResult;
    private MockResultCallback<T> mMockResultOnSuccessCallback;
    protected boolean mDisableAuthorizeCheck = false;

    public AliyunDriveCall(T mockResult) {
        this.mCall = null;
        this.mEntityBeanType = null;
        this.mMockResult = mockResult;
    }

    public AliyunDriveCall(Call call) {
        this.mCall = call;
        this.mEntityBeanType = ((Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    public AliyunDriveCall(Call call, Class<? extends AliyunDriveResponse.GenericMessageInfo> classOfT) {
        this.mCall = call;
        this.mEntityBeanType = classOfT;
    }

    public T execute() {
        T mockResult = this.mMockResult;
        if (mockResult != null) {
            return mockResult;
        }
        try {
            Response response = this.mCall.execute();
            String content = response.body().string();
            if (response.code() == 204) {
                content = "{}";
            }
            T t = (T) JsonUtils.fromJson(content, this.mEntityBeanType);
            checkAuthorize(t);
            MockResultCallback<T> mockResultOnSuccessCallback = this.mMockResultOnSuccessCallback;
            if (mockResultOnSuccessCallback != null) {
                t = mockResultOnSuccessCallback.onSuccess(t);
            }
            return t;
        } catch (NotAuthenticatedException e) {
            throw e;
        } catch (Throwable t) {
            return handleResponseError(t);
        }
    }

    public void checkAuthorize(T t) {
        if (!t.isError()) {
            return;
        }
        if (this.mDisableAuthorizeCheck) {
            return;
        }
        if ("AccessTokenInvalid".equals(t.getCode())) {
            throw new NotAuthenticatedException(t.getMessage() + "(" + t.getCode() + ")");
        }
        if ("TokenExpired".equals(t.getCode())) {
            throw new NotAuthenticatedException(t.getMessage() + "(" + t.getCode() + ")");
        }
    }

    public AliyunDriveCall<T> disableAuthorizeCheck() {
        this.mDisableAuthorizeCheck = true;
        return this;
    }

    public AliyunDriveCall<T> enableAuthorizeCheck() {
        this.mDisableAuthorizeCheck = false;
        return this;
    }

    public void enqueue(final Callback<T> callback) {
        T mockResult = this.mMockResult;
        if (mockResult != null) {
            callback.onResponse(null, null, mockResult);
            return;
        }
        this.mCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(call, e, handleResponseError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String content = response.body().string();
                    if (response.code() == 204) {
                        content = "{}";
                    }
                    T t = (T) JsonUtils.fromJson(content, AliyunDriveCall.this.mEntityBeanType);
                    checkAuthorize(t);
                    MockResultCallback<T> mockResultOnSuccessCallback = AliyunDriveCall.this.mMockResultOnSuccessCallback;
                    if (mockResultOnSuccessCallback != null) {
                        t = mockResultOnSuccessCallback.onSuccess(t);
                    }
                    callback.onResponse(call, response, t);
                    return;
                } catch (Throwable t) {
                    callback.onFailure(call, t, handleResponseError(t));
                    return;
                }
            }
        });
    }

    public AliyunDriveCall<T> mockResultOnSuccess(MockResultCallback<T> callback) {
        this.mMockResultOnSuccessCallback = callback;
        return this;
    }

    private T handleResponseError(Throwable t) {
        try {
            AliyunDriveResponse.GenericMessageInfo genericMessageInfo = this.mEntityBeanType.newInstance();
            genericMessageInfo.setCode("NetworkError");
            genericMessageInfo.setCode(t.toString());
            return (T)genericMessageInfo;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface Callback<T> {
        void onResponse(Call call, Response response, T res);
        void onFailure(Call call, Throwable t, T res);
    }

    public interface MockResultCallback<T> {
        T onSuccess(T res);
    }
}
