import play.http.DefaultHttpFilters;
import play.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import play.http.HttpFilters;
import play.filters.headers.SecurityHeadersFilter;


import javax.inject.Inject;

public class Filters extends DefaultHttpFilters {

        @Inject public Filters(SecurityHeadersFilter securityHeadersFilter) {
            super(securityHeadersFilter);
        }
    }