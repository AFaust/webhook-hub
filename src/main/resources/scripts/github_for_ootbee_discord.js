'use strict';

// TODO Implement other event handlers

function handleIssueEvent(requestData)
{
    var discordRequestMessage = {
        username : requestData.sender.login,
        avatar_url : requestData.sender.avatar_url
    };

    switch (requestData.action)
    {
        case 'opened':
            discordRequestMessage.content = 'A new issue has been created for ' + requestData.repository.full_name + ': "'
                    + requestData.issue.title + '" (' + requestData.issue.html_url + ')';
            break;
        case 'edited':
            if (requestData.changes.title && requestData.changes.title.from !== requestData.issue.title && requestData.changes.body
                    && requestData.changes.body.from !== requestData.issue.body)
            {
                discordRequestMessage.content = 'The issue "' + requestData.changes.title.from + '" for ' + requestData.repository.full_name
                        + ' created by ' + requestData.issue.user.login + ' (previously named "' + requestData.issue.title
                        + '") has been rephrased (' + requestData.issue.html_url + ')';
            }
            else if (requestData.changes.title && requestData.changes.title.from !== requestData.issue.title)
            {
                discordRequestMessage.content = 'The issue "' + requestData.changes.title.from + '" for ' + requestData.repository.full_name
                        + ' created by ' + requestData.issue.user.login + ' has been renamed to "' + requestData.issue.title + '" ('
                        + requestData.issue.html_url + ')';
            }
            else if (requestData.changes.body && requestData.changes.body.from !== requestData.issue.body)
            {
                discordRequestMessage.content = 'The issue "' + requestData.issue.title + '" for ' + requestData.repository.full_name
                        + ' created by ' + requestData.issue.user.login + ' has been rephrased (' + requestData.issue.html_url + ')';
            }
            else
            {
                discordRequestMessage.content = 'The issue "' + requestData.issue.title + '" for ' + requestData.repository.full_name
                        + ' created by ' + requestData.issue.user.login + ' has been edited (' + requestData.issue.html_url + ')';
            }
            break;
        case 'closed':
            discordRequestMessage.content = 'The issue "' + requestData.issue.title + '" for ' + requestData.repository.full_name
                    + ' created by ' + requestData.issue.user.login + ' has been closed (' + requestData.issue.html_url + ')';
            break;
        case 'reopened':
            discordRequestMessage.content = 'The issue "' + requestData.issue.title + '" for ' + requestData.repository.full_name
                    + ' created by ' + requestData.issue.user.login + ' has been reopened (' + requestData.issue.html_url + ')';
            break;
    }

    if (discordRequestMessage.content)
    {
        send(JSON.stringify(discordRequestMessage));
    }
    else
    {
        skip();
    }
}

function handlePullRequestEvent(requestData)
{
    var discordRequestMessage = {
        username : requestData.sender.login,
        avatar_url : requestData.sender.avatar_url
    };

    switch (requestData.action)
    {
        case 'opened':
            discordRequestMessage.content = 'A new pull request has been created for ' + requestData.repository.full_name + ': "'
                    + requestData.pull_request.title + '" (' + requestData.pull_request.html_url + ')';
            break;
        case 'edited':
            if (requestData.changes.title && requestData.changes.title.from !== requestData.pull_request.title && requestData.changes.body
                    && requestData.changes.body.from !== requestData.pull_request.body)
            {
                discordRequestMessage.content = 'The pull request "' + requestData.changes.title.from + '" for '
                        + requestData.repository.full_name + ' created by ' + requestData.pull_request.user.login + ' (previously named "'
                        + requestData.pull_request.title + '") has been rephrased (' + requestData.pull_request.html_url + ')';
            }
            else if (requestData.changes.title && requestData.changes.title.from !== requestData.pull_request.title)
            {
                discordRequestMessage.content = 'The pull request "' + requestData.changes.title.from + '" for '
                        + requestData.repository.full_name + ' created by ' + requestData.pull_request.user.login + ' has been renamed to "'
                        + requestData.pull_request.title + '" (' + requestData.pull_request.html_url + ')';
            }
            else if (requestData.changes.body && requestData.changes.body.from !== requestData.pull_request.body)
            {
                discordRequestMessage.content = 'The pull request "' + requestData.pull_request.title + '" for '
                        + requestData.repository.full_name + ' created by ' + requestData.pull_request.user.login + ' has been rephrased ('
                        + requestData.pull_request.html_url + ')';
            }
            else
            {
                discordRequestMessage.content = 'The pull request "' + requestData.pull_request.title + '" for '
                        + requestData.repository.full_name + ' created by ' + requestData.pull_request.user.login + ' has been edited ('
                        + requestData.pull_request.html_url + ')';
            }
            break;
        case 'closed':
            discordRequestMessage.content = 'The pull request "' + requestData.pull_request.title + '" for ' + requestData.repository.full_name
                    + ' created by ' + requestData.pull_request.user.login + ' has been closed '
                    + (requestData.pull_request.base.merged ? 'after' : 'without') + ' merging changes ('
                    + requestData.pull_request.html_url + ')';
            break;
        case 'reopened':
            discordRequestMessage.content = 'The pull request "' + requestData.pull_request.title + '" for ' + requestData.repository.full_name
                    + ' created by ' + requestData.pull_request.user.login + ' has been reopened (' + requestData.pull_request.html_url
                    + ')';
            break;
        case 'synchronize':
            discordRequestMessage.content = 'The pull request "' + requestData.pull_request.title + '" for ' + requestData.repository.full_name
                    + ' created by ' + requestData.pull_request.user.login + ' has been synchronized (' + requestData.pull_request.html_url
                    + ')';
            break;
    }

    if (discordRequestMessage.content)
    {
        send(JSON.stringify(discordRequestMessage));
    }
    else
    {
        skip();
    }
}

function main()
{
    var eventHandles, eventHeader, requestData;

    eventHandles = {
        'issues' : handleIssueEvent,
        'pull_request' : handlePullRequestEvent
    };
    eventHeader = headers['X-GitHub-Event'];

    if (eventHeader && eventHandles[eventHeader])
    {
        requestData = JSON.parse(content);
        eventHandles[eventHeader](requestData);
    }
    else
    {
        skip();
    }
}

main();
