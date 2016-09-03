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

package net.krotscheck.kangaroo.database.mapper;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An exception mapper that handles hibernate exceptions. At this time, we're
 * not really special casing anything. We'll make modifications as we progress.
 *
 * @author Michael Krotscheck
 */
public final class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    /**
     * Convert to response.
     *
     * @param exception The exception to convert.
     * @return A Response instance for this error.
     */
    public Response toResponse(final ConstraintViolationException exception) {
        return ErrorResponseBuilder
                .from(exception)
                .build();
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(ConstraintViolationExceptionMapper.class)
                    .to(ExceptionMapper.class)
                    .in(Singleton.class);
        }
    }
}
