/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3.internal.http;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.Transmitter;

import static okhttp3.internal.Util.checkDuration;

/**
 * A concrete interceptor chain that carries the entire interceptor chain: all application
 * interceptors, the OkHttp core, all network interceptors, and finally the network caller.
 *
 * <p>If the chain is for an application interceptor then {@link #connection} must be null.
 * Otherwise it is for a network interceptor and {@link #connection} must be non-null.
 */

/**
 * 真正的拦截器执行链
 *
 * @author 马世豪 29350
 * @time 2019/3/27 11:21
 */
public final class RealInterceptorChain implements Interceptor.Chain {
    private final List<Interceptor> interceptors;
    private final Transmitter transmitter;
    private final @Nullable
    Exchange exchange;
    private final int index;
    private final Request request;
    private final Call call;
    private final int connectTimeout;
    private final int readTimeout;
    private final int writeTimeout;
    private int calls;

    public RealInterceptorChain(List<Interceptor> interceptors, Transmitter transmitter,
                                @Nullable Exchange exchange, int index, Request request, Call call,
                                int connectTimeout, int readTimeout, int writeTimeout) {
        this.interceptors = interceptors;
        this.transmitter = transmitter;
        this.exchange = exchange;
        this.index = index;
        this.request = request;
        this.call = call;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
    }

    @Override
    public @Nullable
    Connection connection() {
        return exchange != null ? exchange.connection() : null;
    }

    @Override
    public int connectTimeoutMillis() {
        return connectTimeout;
    }

    @Override
    public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
        int millis = checkDuration("timeout", timeout, unit);
        return new RealInterceptorChain(interceptors, transmitter, exchange, index, request, call,
                millis, readTimeout, writeTimeout);
    }

    @Override
    public int readTimeoutMillis() {
        return readTimeout;
    }

    @Override
    public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
        int millis = checkDuration("timeout", timeout, unit);
        return new RealInterceptorChain(interceptors, transmitter, exchange, index, request, call,
                connectTimeout, millis, writeTimeout);
    }

    @Override
    public int writeTimeoutMillis() {
        return writeTimeout;
    }

    @Override
    public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
        int millis = checkDuration("timeout", timeout, unit);
        return new RealInterceptorChain(interceptors, transmitter, exchange, index, request, call,
                connectTimeout, readTimeout, millis);
    }

    public Transmitter transmitter() {
        return transmitter;
    }

    public Exchange exchange() {
        if (exchange == null) throw new IllegalStateException();
        return exchange;
    }

    @Override
    public Call call() {
        return call;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public Response proceed(Request request) throws IOException {
        return proceed(request, transmitter, exchange);
    }

    public Response proceed(Request request, Transmitter transmitter, @Nullable Exchange exchange)
            throws IOException {
        //拦截最后一个  interceptor
        //防止数据越界
        if (index >= interceptors.size()) throw new AssertionError();

        calls++;

        // 所有拦截器必须只想相同的 地址
        // If we already have a stream, confirm that the incoming request will use it.
        if (this.exchange != null && !this.exchange.connection().supportsUrl(request.url())) {
            throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
                    + " must retain the same host and port");
        }

        // 拦截器只执行一次
        // If we already have a stream, confirm that this is the only call to chain.proceed().
        if (this.exchange != null && calls > 1) {
            throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
                    + " must call proceed() exactly once");
        }
        // 顺序去除 interceptors 里面拦截器，生成， RealInterceptor 执行
        // Call the next interceptor in the chain.
        RealInterceptorChain next = new RealInterceptorChain(interceptors, transmitter, exchange,
                index + 1, request, call, connectTimeout, readTimeout, writeTimeout);
        Interceptor interceptor = interceptors.get(index);
        Response response = interceptor.intercept(next);

        //确认下一个拦截器调用了chain.proceed()
        // Confirm that the next interceptor made its required call to chain.proceed().
        if (exchange != null && index + 1 < interceptors.size() && next.calls != 1) {
            throw new IllegalStateException("network interceptor " + interceptor
                    + " must call proceed() exactly once");
        }

        // 确认 Response 不能为空
        // Confirm that the intercepted response isn't null.
        if (response == null) {
            throw new NullPointerException("interceptor " + interceptor + " returned null");
        }

        if (response.body() == null) {
            throw new IllegalStateException(
                    "interceptor " + interceptor + " returned a response with no body");
        }

        return response;
    }
}
