# Set backend for environment
include "backend.vcl";

# Only allow purge/ban from localhost
acl purgers {
	"127.0.0.1";
}

sub vcl_recv {
	if (req.request == "PURGE") {
		if (!client.ip ~ purgers) {
			error 405 "Method not allowed";
		}
		return (lookup); # Purge will be handled by vcl_hit/vcl_miss
	}

	if (req.request == "BAN") {
		if (!client.ip ~ purgers) {
			error 405 "Method not allowed";
		}

		# Clear any cached object containing the req.url (assuming it's a regex)
		ban("obj.http.x-url ~ " + req.url);

		error 200 "Banned";
	}

	# Set host for environment (to enable virtualhost routing)
	include "host.vcl";

	# We only deal with GET and HEAD by default
	if (req.request != "GET" && req.request != "HEAD") {
		return(pass);
	}

	# Don't cache elasticsearch queries
	if (req.url ~ "/-/") {
		return (pass);
	}
}

sub vcl_hit {
	if (req.request == "PURGE") {
		purge;
		error 200 "Purged";
	}
}

sub vcl_miss {
	# Ensure that all variants of resource are removed from cache
	if (req.request == "PURGE") {
		purge;
		error 200 "Purged";
	}
}

sub vcl_pass {
	if (req.request == "PURGE") {
		error 502 "PURGE on a passed object";
	}
}

sub vcl_fetch {
	# Enable smart bans
	set beresp.http.x-url = req.url;

	# Cache everything 1 year, i.e. only remove objects manually by ban/purge
	if (beresp.ttl > 0s) {
		unset beresp.http.expires;                      # Remove Expires from backend, it's not long enough
		set beresp.http.cache-control = "max-age=900";  # Set the clients TTL on this object
		set beresp.ttl = 52w;                           # Set how long Varnish will keep it
		set beresp.http.magicmarker = "1";              # marker for vcl_deliver to reset Age
	}
}

sub vcl_deliver {
	# Enable smart bans
	unset resp.http.x-url;

	if (resp.http.magicmarker) {
		unset resp.http.magicmarker;  # Remove the magic marker
		set resp.http.age = "0";      # By definition we have a fresh object
	}
}