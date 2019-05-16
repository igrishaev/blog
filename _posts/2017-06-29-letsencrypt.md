---
layout: post
title:  "Let's encrypt"
permalink: /en/letsencrypt
tags: ssl letsencrypt security
lang: en
---

[letsencrypt]: https://letsencrypt.org/
[wiki]: https://en.wikipedia.org/wiki/Certificate_authority

[certbot]: https://certbot.eff.org/

[client-options]: https://letsencrypt.org/docs/client-options/

[docs]: https://certbot.eff.org/docs/


I've just tried [Let's encrypt][letsencrypt] service and may say it works like a
charm! I am really impressed by it's simplicity and robustness. It really works
as it's promised within several lines in shell. That's how a good software
should be made.

Let's encrypt is an [SSL authority service][wiki] that issues short-term SSL
certificates for you. A typical certificate expires in 90 days and then you
request for a new one.

What's the point to use exactly Let's encrypt? There are some other SSL
providers who also offer free certificates, just google for "free SSL cert". The
main reason is Let's encrypt is totally automated. You don't even need to open
their site. The whole setup might be done in bash session in 5 minutes.

Here is a quick example of setting up a SSL certificate on outdated Ubuntu 12.04
LTS:

1. Download `certbot` script. [Certbot][certbot] is an open source software to
   communicate with Let's encrypt service via secure ACME protocol:

   ~~~bash
   wget https://dl.eff.org/certbot-auto
   chmod a+x certbot-auto
   ~~~

2. Backup your Nginx config by copying your `*.conf` files from
   `/etc/nginx/conf.d/` somewhere. Then run:

   ~~~bash
   sudo /path/to/certbot-auto --nginx
   ~~~

   This command will ask you several questions. In most cases, the default
   choice would be enough. It scans your current Nginx config and makes required
   changes. Finally, you will be prompted for submitting your email. Please
   enter an existing one since it requires confirmation. In a minute, check your
   inbox and follow the secret link to submit your account.

3. Reload Nginx service with something like

   ~~~bash
   sudo service nginx restart
   ~~~

   Open your site in Chrome, go to Developer console, "Security" tab, "View
   certificate" below the green label:

   ![SSL green label](/assets/static/ssl-green.png)

   First, all the labels should be green but not red or orange. Second, "Let's
   encrypt" authority should be noticed in the certificate's details:

   ![SSL issued by](/assets/static/ssl-issued.png)

4. You have gone through the main steps so far, although it would be great to
   setup automatic update for your certificate. Add the following into `crontab`
   config:

   ~~~crontab
   0 */12 * * * /path/to/certbot-auto renew --no-self-upgrade
   ~~~

   This job tries to update the certificate twice a day as the official guide
   recommends.

To find out more, please examine the [Certbot documentation][docs]. It has nice
setup wizard with step-by-step algorithms for all the operation systems. You may
also automate Let's encrypt not with bash script but within your favorite
language. See the ["Client options"][client-options] page to observe existing
libraries.

Finally, I urge you to enable SSL for your project right now if you haven't done
this yet. Nowadays, there cannot be an excuse for sending your client's data
as-is without encryption. Please respect your visitors. Setting up SSL has never
been so easy as it is with Let's encrypt nowadays.
