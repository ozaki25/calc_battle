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
      when 'updateUser'
        for uid, continuationCorrect of message.user
          $("#uid_#{uid}").empty()
          updateStar(uid, continuationCorrect)
          finishEffect(uid) if message.finish
      when 'participation'
        $('#users').html(
          for uid in message.uids
            "<li class=\"list-group-item\">ユーザ#{uid}</li>"
        )
      else
        console.log "[Error] unmatch message: #{message}"

  updateStar = (uid, continuationCorrect) ->
    $("#uid_#{uid}").append "ユーザ#{uid} "
    unless continuationCorrect is 0
      for i in [1..continuationCorrect]
        $("#uid_#{uid}").append "<span class=\"glyphicon glyphicon-star\" aria-hidden=\"true\"></span>"
    unless continuationCorrect is 5
      for i in [1..(5 - continuationCorrect)]
        $("#uid_#{uid}").append "<span class=\"glyphicon glyphicon-star-empty\" aria-hidden=\"true\"></span>"

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
