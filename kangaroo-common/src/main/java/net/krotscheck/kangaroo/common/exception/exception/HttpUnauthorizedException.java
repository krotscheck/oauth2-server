/*
 * Copyright (c) 2016 Michael Krotscheck
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.krotscheck.kangaroo.common.exception.exception;

import org.apache.http.HttpStatus;

import java.net.URI;

/**
 * Convenience exception for 401-unauthorized.
 *
 * @author Michael Krotscheck
 */
public final class HttpUnauthorizedException extends HttpStatusException {

    /**
     * Create a new HttpUnauthorizedException.
     */
    public HttpUnauthorizedException() {
        super(HttpStatus.SC_UNAUTHORIZED);
    }

    /**
     * Create a new redirecting HttpNotFoundException.
     *
     * @param redirect The URI to send the user agent to.
     */
    public HttpUnauthorizedException(final URI redirect) {
        super(HttpStatus.SC_UNAUTHORIZED, redirect);
    }
}