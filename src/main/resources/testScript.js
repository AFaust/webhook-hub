/* globals uriVariables:false skip:false logger:false */
'use strict';

if (uriVariables.event === 'fork')
{
    skip();
}
else
{
    send(JSON.stringify({
        username : 'AFaust',
        content : 'Just some dummy test content from within AFaust\'s webhook-hub server'
    }));
}
