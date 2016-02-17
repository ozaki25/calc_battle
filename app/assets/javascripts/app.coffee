$ ->
  ws = new WebSocket $('body').data 'ws-url'
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    console.log message
    switch message.type
      when 'question'
        first = message.question.first
        second = message.question.second
        $('#question').html "#{first} + #{second}"
        $('#answer').attr 'answer', first + second
      when 'updateUser'
        user = message.user
        $("#uid_#{user.uid}").empty()
        updateStar(user)
        finishEffect(user.uid) if message.finish
      when 'updateUsers'
        $('#users').empty()
        for user in message.users
          $('#users').append "<li id=\"uid_#{user.uid}\" class=\"list-group-item\"></li>"
          updateStar(user)
      else
        console.log "[Error] unmatch message: #{message}"

  updateStar = (user) ->
    $("#uid_#{user.uid}").append "ユーザ#{user.uid} "
    unless user.correctCount is 0
      for i in [1..user.correctCount]
        $("#uid_#{user.uid}").append "<span class=\"glyphicon glyphicon-star\" aria-hidden=\"true\"></span>"
    unless user.correctCount is 5
      for i in [1..(5 - user.correctCount)]
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
    $('#answer').removeAttr 'disabled'
    ws.send JSON.stringify { start: true }
