/*
Android Asynchronous Http Client
Copyright (c) 2011 James Smith <james@loopj.com>
http://loopj.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.blazeroni.reddit.http;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

/**
* A wrapper class around {@link Cookie} and/or {@link BasicClientCookie}
* designed for use in {@link PersistentCookieStore}.
*/
public class SerializableCookie implements Serializable {
    private static final long serialVersionUID = 6374381828722046732L;

    private transient Cookie cookie;

    public SerializableCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public Cookie getCookie() {
    	return this.cookie;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(this.cookie.getName());
        out.writeObject(this.cookie.getValue());
        out.writeObject(this.cookie.getComment());
        out.writeObject(this.cookie.getDomain());
        out.writeObject(this.cookie.getExpiryDate());
        out.writeObject(this.cookie.getPath());
        out.writeInt(this.cookie.getVersion());
        out.writeBoolean(this.cookie.isSecure());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String name = (String)in.readObject();
        String value = (String)in.readObject();
        BasicClientCookie c = new BasicClientCookie(name, value);
        c.setComment((String)in.readObject());
        c.setDomain((String)in.readObject());
        c.setExpiryDate((Date)in.readObject());
        c.setPath((String)in.readObject());
        c.setVersion(in.readInt());
        c.setSecure(in.readBoolean());
        
        this.cookie = c;
    }
}