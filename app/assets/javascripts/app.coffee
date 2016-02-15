$ ->
  ws = new WebSocket $('body').data 'ws-url'
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.type
      when 'question'
        first = message.question.first
        second = message.question.second
        $('#question').html "#{first} + #{second}"
        $('#answer').attr 'answer', first + second
        console.log "#{first} + #{second}"
      when 'updateUser'
        user = message.user
        $("#uid_#{user.uid}").empty()
        updateStar(user)
        finishEffect(user.uid) if message.finish
        console.log "uid: #{user.uid}, continuationCorrect: #{user.continuationCorrect}, finish: #{message.finish}"
      when 'participation'
        $('#users').html(
          for uid in message.uids
            "<li id=\"uid_#{uid}\" class=\"list-group-item\">ユーザ#{uid}</li>"
        )
        console.log "uids: #{message.uids}"
      else
        console.log "[Error] unmatch message: #{message}"

  updateStar = (user) ->
    $("#uid_#{user.uid}").append "ユーザ#{user.uid} "
    unless user.continuationCorrect is 0
      for i in [1..user.continuationCorrect]
        $("#uid_#{user.uid}").append "<span class=\"glyphicon glyphicon-star\" aria-hidden=\"true\"></span>"
    unless user.continuationCorrect is 5
      for i in [1..(5 - user.continuationCorrect)]
        $("#uid_#{user.uid}").append "<span class=\"glyphicon glyphicon-star-empty\" aria-hidden=\"true\"></span>"

  finishEffect = (uid) ->
    $('#answer').attr 'disabled', 'disabled'
    $("#uid_#{uid}").addClass 'list-group-item-success'

  $(document).on 'keypress', '#answer', (e) ->
    ENTER = 13
    if e.which is ENTER
      input = $(this).val().trim()
      answer = $(this).attr 'answer'
      return unless input
      ws.send JSON.stringify { result: input is answer }
      $(this).val ''

  $('#start').click ->
      ws.send JSON.stringify { start: true }
