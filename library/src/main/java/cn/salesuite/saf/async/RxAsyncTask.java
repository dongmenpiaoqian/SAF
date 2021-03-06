package cn.salesuite.saf.async;

import android.app.Dialog;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Tony Shen on 2016/11/9.
 */

public abstract class RxAsyncTask<T> {

    private Dialog mDialog;
    private SuccessHandler successHandler;
    private FailedHandler failedHandler;
    private int retryCount = 3;

    public RxAsyncTask() {
        this(null);
    }

    public RxAsyncTask(Dialog dialog) {
        this.mDialog = dialog;
    }

    private void onPreExecute() {
        if (mDialog != null) {
            mDialog.show();
        }
    }

    private void execute() {
        onPreExecute();
        Observable<T> observable = createObservable();

        if (retryCount > 0) {
            observable.retryWhen(new RetryWithDelay(retryCount, 1000))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<T>() {
                        @Override
                        public void call(T t) {
                            if (mDialog != null) {
                                mDialog.dismiss();
                                mDialog = null;
                            }

                            if (successHandler != null) {
                                successHandler.onSuccess(t);
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            if (mDialog != null) {
                                mDialog.dismiss();
                                mDialog = null;
                            }

                            if (throwable != null && failedHandler != null) {
                                failedHandler.onFail(throwable);
                            }
                        }
                    });
        } else {
            observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<T>() {
                        @Override
                        public void call(T t) {
                            if (mDialog != null) {
                                mDialog.dismiss();
                                mDialog = null;
                            }

                            if (successHandler != null) {
                                successHandler.onSuccess(t);
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            if (mDialog != null) {
                                mDialog.dismiss();
                                mDialog = null;
                            }

                            if (throwable != null && failedHandler != null) {
                                failedHandler.onFail(throwable);
                            }
                        }
                    });
        }

    }

    /**
     * 所有的task都必须执行这个方法，否则无法运行
     */
    public void start(){
        execute();
    }

    private Observable<T> createObservable() {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {

                subscriber.onNext(onExecute());
                subscriber.onCompleted();
            }
        });
    }

    public RxAsyncTask success(SuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    public RxAsyncTask failed(FailedHandler failedHandler) {
        this.failedHandler = failedHandler;
        return this;
    }

    public RxAsyncTask retry(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    /**
     * 执行任务的结果
     *
     * @return
     */
    public abstract T onExecute();

    public interface SuccessHandler<T> {

        void onSuccess(T t);
    }

    public interface FailedHandler {

        void onFail(Throwable e);
    }
}
