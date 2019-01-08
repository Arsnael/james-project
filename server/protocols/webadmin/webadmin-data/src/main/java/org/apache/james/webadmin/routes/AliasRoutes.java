package org.apache.james.webadmin.routes;

import static org.apache.james.webadmin.Constants.SEPARATOR;
import static spark.Spark.halt;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.james.core.MailAddress;
import org.apache.james.core.User;
import org.apache.james.rrt.api.MappingAlreadyExistsException;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.MappingSource;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Service;

@Api(tags = "Address Aliases")
@Path(AliasRoutes.ROOT_PATH)
@Produces(Constants.JSON_CONTENT_TYPE)
public class AliasRoutes implements Routes {

    public static final String ROOT_PATH = "address/aliases";

    private static final Logger LOGGER = LoggerFactory.getLogger(AliasRoutes.class);

    private static final String ALIAS_DESTINATION_ADDRESS = "aliasDestinationAddress";
    private static final String ALIAS_ADDRESS_PATH = ROOT_PATH + SEPARATOR + ":" + ALIAS_DESTINATION_ADDRESS;
    private static final String ALIAS_SOURCE_ADDRESS = "aliasSourceAddress";
    private static final String USER_IN_ALIAS_SOURCES_ADDRESSES_PATH = ALIAS_ADDRESS_PATH + SEPARATOR +
        "sources" + SEPARATOR + ":" + ALIAS_SOURCE_ADDRESS;
    private static final String MAILADDRESS_ASCII_DISCLAIMER = "Note that email addresses are restricted to ASCII character set. " +
        "Mail addresses not matching this criteria will be rejected.";

    private final UsersRepository usersRepository;
    private final RecipientRewriteTable recipientRewriteTable;

    @Inject
    @VisibleForTesting
    AliasRoutes(RecipientRewriteTable recipientRewriteTable, UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
        this.recipientRewriteTable = recipientRewriteTable;
    }

    @Override
    public String getBasePath() {
        return ROOT_PATH;
    }

    @Override
    public void define(Service service) {
        service.put(ALIAS_ADDRESS_PATH, this::throwUnknownPath);
        service.put(USER_IN_ALIAS_SOURCES_ADDRESSES_PATH, this::addToAliasSources);
    }

    public Object throwUnknownPath(Request request, Response response) {
        throw ErrorResponder.builder()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
            .message("An alias source needs to be specified in the path")
            .haltError();
    }

    @PUT
    @Path(ROOT_PATH + "/{" + ALIAS_DESTINATION_ADDRESS + "}/sources/{" + ALIAS_SOURCE_ADDRESS + "}")
    @ApiOperation(value = "adding a source address into an alias")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = ALIAS_DESTINATION_ADDRESS, paramType = "path",
            value = "Destination mail address of the alias. Sending a mail to the alias source address will send it to " +
                "that email address.\n" +
                MAILADDRESS_ASCII_DISCLAIMER),
        @ApiImplicitParam(required = true, dataType = "string", name = ALIAS_SOURCE_ADDRESS, paramType = "path",
            value = "Source mail address of the alias. Sending a mail to that address will send it to " +
                "the email destination address.\n" +
                MAILADDRESS_ASCII_DISCLAIMER)
    })
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.NO_CONTENT_204, message = "OK"),
        @ApiResponse(code = HttpStatus.BAD_REQUEST_400, message = ALIAS_DESTINATION_ADDRESS + " or alias structure format is not valid"),
        @ApiResponse(code = HttpStatus.BAD_REQUEST_400, message = "The alias source exists as an user already"),
        @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500,
            message = "Internal server error - Something went bad on the server side.")
    })
    public HaltException addToAliasSources(Request request, Response response) throws UsersRepositoryException, RecipientRewriteTableException {
        MailAddress aliasSourceAddress = parseMailAddress(request.params(ALIAS_SOURCE_ADDRESS));
        ensureUserDoesNotExist(aliasSourceAddress);
        MailAddress destinationAddress = parseMailAddress(request.params(ALIAS_DESTINATION_ADDRESS));
        MappingSource source = MappingSource.fromUser(User.fromLocalPartWithDomain(destinationAddress.getLocalPart(), destinationAddress.getDomain()));
        addAlias(source, aliasSourceAddress);
        return halt(HttpStatus.NO_CONTENT_204);
    }

    private void addAlias(MappingSource source, MailAddress aliasSourceAddress) throws RecipientRewriteTableException {
        try {
            recipientRewriteTable.addAliasMapping(source, aliasSourceAddress.asString());
        } catch (MappingAlreadyExistsException e) {
            // ignore
        }
    }

    private void ensureUserDoesNotExist(MailAddress mailAddress) throws UsersRepositoryException {
        if (usersRepository.contains(mailAddress.asString())) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .message("The alias source exists as an user already")
                .haltError();
        }
    }

    private MailAddress parseMailAddress(String address) {
        try {
            String decodedAddress = URLDecoder.decode(address, StandardCharsets.UTF_8.displayName());
            return new MailAddress(decodedAddress);
        } catch (AddressException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .message("The alias is not an email address")
                .cause(e)
                .haltError();
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("UTF-8 should be a valid encoding");
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .type(ErrorResponder.ErrorType.SERVER_ERROR)
                .message("Internal server error - Something went bad on the server side.")
                .cause(e)
                .haltError();
        }
    }
}
