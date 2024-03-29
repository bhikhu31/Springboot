package deo.trial.Springboot.account;

import deo.trial.Springboot.service.LocalAccountProvider;
import com.google.common.base.Stopwatch;
import org.ctoolkit.restapi.client.RestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The {@link LocalAccountProvider} implementation.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Singleton
public class LocalAccountProviderImpl
        implements LocalAccountProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger( LocalAccountProviderImpl.class );

    private final RestFacade facade;

    @Inject
    LocalAccountProviderImpl( RestFacade facade )
    {
        this.facade = facade;
    }

    @Override
    public LocalAccount initGet( @Nonnull Builder builder )
    {
        checkNotNull( builder, "Builder can't be null" );
        checkNotNull( builder.getEmail(), "Account email can't be null" );
        checkNotNull( builder.getIdentityId(), "Account Identity ID is mandatory" );

        LocalAccount localAccount;
        if ( builder.getAccountId() == null )
        {
            localAccount = get( builder.getEmail() );
        }
        else
        {
            // Retrieval with Account ID performs better and is cheaper thus preferred
            localAccount = get( builder.getAccountId() );
        }

        if ( localAccount == null )
        {
            Stopwatch stopwatch = Stopwatch.createStarted();
            localAccount = new LocalAccount( builder );
            localAccount.init( facade );
            localAccount.save();
            stopwatch.stop();
            LOGGER.info( "Local account just has been created (" + stopwatch + "): " + localAccount );
        }

        return localAccount;
    }

    @Override
    public LocalAccount get( @Nonnull String email )
    {
        checkNotNull( email, "Account email can't be null" );

        return ofy()
                .load()
                .type( LocalAccount.class )
                .filter( "email", email )
                .first()
                .now();
    }

    @Override
    public LocalAccount get( @Nonnull Long id )
    {
        return ofy().load().type( LocalAccount.class ).id( id ).now();
    }
}
