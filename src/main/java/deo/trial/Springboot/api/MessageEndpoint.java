package deo.trial.Springboot.api;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiReference;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Endpoint REST API for {@link Message} content resource.
 * For more info regarding authentication see {@link EndpointsApiProfile}.
 * <p>
 * These are the injected types you can optionally let Endpoints Framework inject to the API method:
 * <ul>
 * <li>{@link ServletContext}</li>
 * <li>{@link HttpServletRequest}</li>
 * </ul>
 * Next, one of the following authenticated User might be injected:
 * <ul>
 * <li>{@link com.google.appengine.api.users.User} coming from App Engine API library</li>
 * <li>or {@link com.google.api.server.spi.auth.common.User} coming from Endpoints Framework library </li>
 * </ul>
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Api
@ApiReference( EndpointsApiProfile.class )
public class MessageEndpoint
{
    private final EndpointsCommon common;

    @Inject
    public MessageEndpoint( EndpointsCommon common )
    {
        this.common = common;
    }

    @ApiMethod( name = "message.update",
            path = "message/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT
    )
    public Message updateMessage( @Named( "id" ) Long id,
                                  Message message,
                                  // Endpoints optionally injected types
                                  HttpServletRequest request,
                                  ServletContext context,
                                  com.google.api.server.spi.auth.common.User authUser )
            throws NotFoundException, InternalServerErrorException, UnauthorizedException
    {
        // uncomment once you have signed-up for TurnOnline.biz Ecosystem account
        //common.checkLocalAccount( authUser );

        String greetings = message.getGreetings() == null ? "" : " " + message.getGreetings();
        message.setGreetings( id + greetings );

        return message;
    }
}
