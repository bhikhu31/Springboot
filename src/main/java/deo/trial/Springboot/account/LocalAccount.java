package deo.trial.Springboot.account;

import deo.trial.Springboot.service.LocalAccountProvider;
import biz.turnonline.ecosystem.steward.facade.Domicile;
import biz.turnonline.ecosystem.steward.model.Account;
import biz.turnonline.ecosystem.steward.model.AccountBusiness;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import org.ctoolkit.restapi.client.NotFoundException;
import org.ctoolkit.restapi.client.RestFacade;
import org.ctoolkit.services.datastore.objectify.EntityLongIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The service local lightweight account.
 * <p>
 * It's a local representation of TurnOnline.biz Ecosystem account managed
 * by Account Steward microservice. It's intended to act as an owner of an entities if needed.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Cache( expirationSeconds = 3600 )
@Entity( name = "REPLACE_PREFIX_LocalAccount" )
public class LocalAccount
        extends EntityLongIdentity
{
    private static final Locale DEFAULT_LOCALE = Domicile.getDefault().getLocale();

    private static final String DEFAULT_DOMICILE = Domicile.getDefault().name();

    private static final Logger LOGGER = LoggerFactory.getLogger( LocalAccount.class );

    private static final long serialVersionUID = 1L;

    private String locale;

    @Index
    private String identityId;

    @Index
    private String email;

    private String zone;

    private String domicile;

    @Ignore
    private transient Account tAccount;

    LocalAccount()
    {
    }

    /**
     * Constructs local account. If no Account ID set then can't be saved.
     *
     * @param builder mandatory properties are: email, identityId
     */
    LocalAccount( @Nonnull LocalAccountProvider.Builder builder )
    {
        checkNotNull( builder, "Builder can't be null" );
        this.email = checkNotNull( builder.getEmail(), "Account email is mandatory" );
        this.identityId = checkNotNull( builder.getIdentityId(), "Account Identity ID is mandatory" );
        super.setId( builder.getAccountId() );
    }

    /**
     * Constructs local account.
     *
     * @param account the TurnOnline.biz Ecosystem remote account
     */
    public LocalAccount( @Nonnull Account account )
    {
        this( new LocalAccountProvider.Builder()
                .accountId( account.getId() )
                .email( account.getEmail() )
                .identityId( account.getIdentityId() ) );

        this.tAccount = account;
    }

    /**
     * Initialize local properties from remote account.
     *
     * @param facade the rest facade, a service to get a remote account
     * @throws NotFoundException if the remote account not found
     */
    void init( @Nonnull RestFacade facade )
    {
        Account account = getAccount( facade );
        init( account );
    }

    private void init( @Nonnull Account account )
    {
        super.setId( account.getId() );
        this.email = account.getEmail();
        this.identityId = account.getIdentityId();
        this.locale = account.getLocale();

        AccountBusiness business = account.getBusiness();
        if ( business != null )
        {
            domicile = business.getDomicile();
        }

        String zoneId = account.getZoneId();
        if ( Strings.isNullOrEmpty( zoneId ) )
        {
            this.zone = "Europe/Paris";
        }
        else
        {
            this.zone = zoneId;
        }
    }

    /**
     * Returns the account unique identification within TurnOnline.biz Ecosystem.
     *
     * @return the account unique identification
     */
    public Long getAccountId()
    {
        return getId();
    }

    /**
     * The login email address of the account.
     *
     * @return the account login email
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * Sets the login email address of the account.
     * <p>
     * In some (probably rare) cases an user might change its login email under the umbrella
     * of the same login provider. In this case the identity Id remains same,
     * while email has been changed.
     *
     * @param email the email to be set, not null
     */
    void setEmail( @Nonnull String email )
    {
        this.email = checkNotNull( email, "Login email can't be null" );
    }

    /**
     * The email account unique identification within third-party login provider.
     *
     * @return the unique identification of the email account
     */
    public String getIdentityId()
    {
        return identityId;
    }

    /**
     * Retrieves remote account identified by {@link #getAccountId()}.
     * Authentication against microservice is via service account
     * on behalf of current email and identityId.
     *
     * @param facade the rest facade, a service to get a remote account
     * @return the remote account
     * @throws NotFoundException if the remote account not found
     */
    public Account getAccount( @Nonnull RestFacade facade )
    {
        if ( tAccount != null )
        {
            return tAccount;
        }

        String loginId;
        Long accountId = getAccountId();
        if ( accountId == null )
        {
            loginId = getIdentityId();
        }
        else
        {
            loginId = accountId.toString();
        }

        return checkNotNull( facade, "REST Facade must be provided" ).get( Account.class )
                .identifiedBy( checkNotNull( loginId, "Account login ID can't be null" ) )
                .onBehalfOf( this )
                .finish();
    }

    /**
     * Returns the account time-zone ID, such as Europe/Paris. Used to identify the rules
     * how to render date time properties of the resources associated with this account.
     *
     * @return the time-zone ID
     */
    public ZoneId getZoneId()
    {
        return ZoneId.of( checkNotNull( zone, "LocalAccount.zone property can't be null" ) );
    }

    /**
     * Sets the time-zone ID.
     *
     * @param zone the time-zone ID to be set, not null
     */
    void setZoneId( @Nonnull String zone )
    {
        this.zone = checkNotNull( zone, "Zone ID can't be null" );
    }

    /**
     * Returns the account locale. Always returns a value.
     * If none of the values has been found a {@link #DEFAULT_LOCALE} will be returned.
     *
     * @return the final locale, ISO 639 alpha-2 or alpha-3 language code
     */
    public Locale getLocale()
    {
        return getLocale( null );
    }

    /**
     * Sets the preferred account language. ISO 639 alpha-2 or alpha-3 language code.
     *
     * @param locale the language to be set
     */
    public void setLocale( String locale )
    {
        this.locale = locale;
    }

    /**
     * Returns the account locale with specified preference.
     * Always returns a value. If none of the values has been found a {@link #DEFAULT_LOCALE} will be returned.
     *
     * @param locale the optional however preferred language
     * @return the final locale
     */
    public Locale getLocale( @Nullable Locale locale )
    {
        return locale != null ? locale : convertJavaLocale( this.locale, DEFAULT_LOCALE );
    }

    /**
     * Returns the account domicile with optional preference. Always returns a value.
     * If none domicile value found a {@link #DEFAULT_DOMICILE} will be returned.
     *
     * @param domicile the optional (preferred) ISO 3166 alpha-2 country code that represents a target domicile
     * @return the account domicile or default
     * @throws IllegalArgumentException if domicile value is none of the supported {@link Domicile}
     */
    public Domicile getDomicile( @Nullable String domicile )
    {
        if ( domicile == null )
        {
            if ( Strings.isNullOrEmpty( this.domicile ) )
            {
                domicile = DEFAULT_DOMICILE;
                LOGGER.warn( "Using service default locale: " + domicile );
            }
            else
            {
                domicile = this.domicile;
            }
        }
        return Domicile.valueOf( domicile.toUpperCase() );
    }

    /**
     * Returns the account domicile. Always returns a value.
     * If none domicile value found a {@link #DEFAULT_DOMICILE} will be returned.
     *
     * @return the account domicile or default
     * @throws IllegalArgumentException if domicile value is none of the supported {@link Domicile}
     */
    public Domicile getDomicile()
    {
        return getDomicile( null );
    }

    /**
     * Sets the ISO 3166 alpha-2 country code that represents account domicile.
     *
     * @param domicile the domicile to be set
     */
    void setDomicile( String domicile )
    {
        this.domicile = domicile;
    }

    @Override
    public void save()
    {
        if ( getId() == null )
        {
            String msg = "The Account ID is being expected to be set in advance from remote Account.";
            throw new IllegalArgumentException( msg );
        }
        ofy().transact( () -> ofy().save().entity( this ).now() );
    }

    @Override
    public void delete()
    {
        ofy().transact( () -> ofy().delete().entity( this ).now() );
    }

    @Override
    protected long getModelVersion()
    {
        //21.10.2017 08:00:00 GMT+0200
        return 1508565600000L;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( !( o instanceof LocalAccount ) ) return false;

        LocalAccount that = ( LocalAccount ) o;
        return Objects.equals( this.getId(), that.getId() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getId() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
                .add( "id", getId() )
                .add( "email", email )
                .add( "identityId", identityId )
                .add( "locale", locale )
                .add( "domicile", domicile )
                .add( "zone", zone )
                .toString();
    }
}
