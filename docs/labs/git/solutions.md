# Подсказки и решения лабораторных Git

[Задания](README.md) · [Теория](../../theory/engineering/git.md)

Ниже — ориентиры для самопроверки, а не скрипт для вставки целиком. Выполняй в песочнице, созданной в лабораторной 0. Обычное начало — alice, main, чистое дерево; исключения указаны в заданиях. Хеши у тебя будут свои. Перед merge и reset проверяй текущую ветку.

## 0. Устройство стенда

`origin` хранит путь к remote.git. alice и bob — самостоятельные клоны с отдельными рабочими файлами, индексами, локальными ветками и настройками. После clone/push их main указывает на один начальный коммит. `git branch -vv` показывает `[origin/main]`.

## 1. Индекс

```powershell
git switch -c lab/staging
Add-Content notes.txt 'prepared'
git add notes.txt
Add-Content notes.txt 'later'
git status --short
git diff
git diff --staged
git commit -m 'Add prepared note'
git show HEAD:notes.txt
git add notes.txt
git commit -m 'Add later note'
Add-Content notes.txt 'temporary'
git add notes.txt
git restore --staged notes.txt
git diff
git restore notes.txt
git switch main
git merge lab/staging
git push
```

Перед первым commit статус notes.txt — `MM`: индекс содержит prepared, рабочая директория содержит ещё и later. После первого commit остаётся неподготовленная later. После restore --staged временная строка остаётся только в рабочем файле; следующий restore убирает её.

## 2. Игнорирование

```powershell
git switch -c lab/ignore
Set-Content fake.env 'TOKEN=demo'
git add fake.env
git commit -m 'Track fake environment for exercise'
Set-Content .gitignore @('fake.env', '*.log')
Set-Content debug.log 'debug'
git add .gitignore
git commit -m 'Ignore local environment and logs'
Add-Content fake.env 'MODE=training'
git status --short
git rm --cached fake.env
git commit -m 'Stop tracking fake environment'
git ls-files fake.env
git check-ignore -v fake.env
git switch main
git merge lab/ignore
git push
```

Правило игнорирования не отменяет отслеживание. В раннем коммите TOKEN=demo остаётся, потому что история не переписывалась. Это учебная строка, а действующий опубликованный секрет пришлось бы заменить.

## 3. Слияние

Создай два разных файла в разошедшихся ветках, затем на main:

```powershell
git merge lab/feature -m 'Merge training feature'
git show --no-patch --format='%h %p %s' HEAD
git log --graph --oneline --all
git push
git branch -d lab/feature
```

Два хеша после хеша merge-коммита — его родители. Fast-forward невозможен, потому что main имеет собственный коммит после ответвления feature. Удаление имени ветки не удаляет включённые в main коммиты.

## 4. Конфликт

После двух разных изменений timeout на main и lab/timeout:

```powershell
git merge lab/timeout
git status
git merge --abort
Get-Content app.properties
git merge lab/timeout
Set-Content app.properties 'timeout=45'
git diff --check
git add app.properties
git commit -m 'Merge timeout settings with agreed value'
git push
```

`git commit` здесь завершает merge, как и `git merge --continue` с вводом сообщения. Merge-коммит должен иметь два родителя. Итог определяется условием задачи, а не тем, какая панель в IDEA названа «моей».

## 5. Stash

```powershell
git switch -c lab/stash
Add-Content notes.txt 'work in progress'
Set-Content draft.txt 'draft'
git stash push -u -m 'Training interruption'
git status
git switch main
git switch lab/stash
git stash list
git stash show -p -u 'stash@{0}'
git stash apply 'stash@{0}'
git add notes.txt draft.txt
git commit -m 'Finish interrupted work'
git stash drop 'stash@{0}'
git switch main
git merge lab/stash
git push
```

Без `-u` новый draft.txt остался бы в рабочей директории. `show -u` показывает также сохранённые untracked-файлы. Apply сохраняет запись: можно проверить результат до удаления. Stash не заменяет опубликованную ветку с коммитами.

## 6. Отказ push

После создания и отправки Alice её коммита, а Bob — своего локального коммита:

```powershell
# В bob
git push
# Ожидается отказ non-fast-forward
git fetch origin
git log --graph --oneline --all
git pull --ff-only
# Ожидается отказ: истории разошлись
git merge origin/main -m 'Integrate Alice and Bob work'
git push
Set-Location (Join-Path $gitLabRoot 'alice')
git pull --ff-only
```

После fetch у Bob меняется origin/main, но не main и не рабочие файлы. После merge итог включает обоих разработчиков. Если сервер запрещает push политикой доступа, интеграция истории не даёт дополнительных прав.

## 7. Rebase

После двух коммитов в lab/rebase, создания backup/lab-rebase и нового коммита main:

```powershell
git switch lab/rebase
git rebase main
git log --graph --oneline --all
git rebase -i HEAD~2
```

В открывшемся todo оставь `pick` у первой строки и замени `pick` на `squash` у второй. Сохрани и закрой редактор. Затем введи общее сообщение и снова сохрани. Список идёт от более старого коммита к более новому. Если открылся Vim: `i` для редактирования, затем Esc, `:wq`, Enter для сохранения и выхода. Можно заранее настроить знакомый редактор локальной настройкой `core.editor`.

```powershell
git rev-list --count main..lab/rebase
Get-Content rebase.txt
git switch main
git merge lab/rebase
git push
```

Хеши меняются из-за новых родителей/содержимого метаданных. Backup сохраняет старую линию, поэтому `log --all` показывает и её — это ожидаемо.

## 8. Отмена

```powershell
git switch -c lab/undo
Set-Content undo.txt 'version-1'
git add undo.txt
git commit -m 'Add undo example'
Set-Content undo.txt 'version-2'
git add undo.txt
git restore --staged undo.txt
git diff
git restore undo.txt
Add-Content undo.txt 'forgotten'
git add undo.txt
git commit --amend --no-edit
Set-Content extra.txt 'extra'
git add extra.txt
git commit -m 'Add extra file'
git reset --soft HEAD~1
git status --short
git commit -m 'Add extra file again'
git reset --mixed HEAD~1
git status --short
git add extra.txt
git commit -m 'Add extra file after mixed reset'
git revert --no-edit HEAD
git log --oneline -5
git switch main
```

Soft сохраняет индекс с добавленным extra.txt. Mixed возвращает индекс к состоянию, где extra.txt ещё нет, но оставляет сам файл: статус `??`. Revert добавляет новый коммит с удалением extra.txt.

## 9. Выборочный перенос

После двух коммитов в lab/hotfix сохрани хеш последнего в переменную:

```powershell
$gitLabFix = git rev-parse HEAD
git switch main
git cherry-pick $gitLabFix
Test-Path hotfix.txt
Test-Path unrelated.txt
git tag -a lab-v1.0.0 -m 'Training release 1.0.0'
git push
git push origin lab-v1.0.0
```

Ожидается True и False. Cherry-pick переносит изменение выбранного коммита, а не всех его предков. Это сработало, потому что исправление независимо от unrelated.txt.

## 10. Reflog

После reset найди коммит с сообщением, которым сохранял rescue.txt:

```powershell
git reflog
# Подставь найденный хеш вместо HASH
git show HASH
git branch rescue/saved HASH
git switch rescue/saved
Get-Content rescue.txt
git switch main
```

Reset передвинул ветку, но коммит ещё доступен через reflog. Новая ветка удерживает его. Если файл никогда не был сохранён Git, reflog не содержит его текст.

## 11. Bisect

Шесть коммитов можно подготовить так, находясь на новой lab/bisect:

```powershell
foreach ($gitLabStep in 1..6) {
    $gitLabPrice = if ($gitLabStep -lt 3) { '100' } else { '200' }
    $gitLabLines = if ($gitLabStep -eq 1) { @($gitLabPrice) } else { @($gitLabPrice, "note=$gitLabStep") }
    Set-Content price.txt $gitLabLines
    git add price.txt
    git commit -m "Price step $gitLabStep"
}
git log --oneline -6
```

Скопируй хеш Price step 1 и используй вместо GOOD_HASH:

```powershell
git bisect start
git bisect bad
git bisect good GOOD_HASH
Get-Content price.txt -TotalCount 1
```

После каждого переключения: если первая строка 100 — `git bisect good`, иначе `git bisect bad`. Когда Git назвал первый плохой коммит, выполни `git bisect reset` и вернись на main. Не копируй фиксированную последовательность good/bad: решение зависит от реально проверяемой ревизии.

## 12. Один возможный маршрут итоговой задачи

Это подсказка к планированию; имена файлов и промежуточные хеши выбери сам.

1. Обнови оба клона. В Alice создай `final/payment`. Первый коммит меняет timeout с 45 на 50, второй добавляет payment-check.txt. Добавь forgotten-check.txt и через amend включи его во второй коммит.
2. В Bob на main поменяй timeout с 45 на 55, закоммить и отправь. В Alice выполни fetch и `git merge origin/main`. Получится конфликт. Итог по договорённости — timeout=60: исправь, добавь файл, заверши merge. Выбран merge, потому что хотим сохранить обе линии; rebase своей неопубликованной ветки тоже был бы допустим.
3. Проверь файлы, `diff --check`, status. Выполни `git push -u origin final/payment`.
4. В Alice обнови main через `git pull --ff-only`; затем `git merge --no-ff final/payment -m 'Merge final payment task'`, отправь main. В обычной команде этот шаг часто выполняет хостинг после review, а здесь его имитируем локально.
5. Создай `final/revert-checks` от main. Через log найди **обычный второй коммит задачи после amend**, добавлявший два check-файла. Выполни `git revert HASH`, проверь их удаление и сохранение timeout=60. Закоммиченный результат уже создан revert. Слей ветку исправления в main и отправь.
6. Создай `git tag -a lab-final -m 'Final lab result'`, отправь `git push origin lab-final`. В Bob обнови main и выполни `git fetch origin --tags`. Сравни HEAD, файл конфигурации и `git status`.
7. На main в Alice удали `final/payment` через `git branch -d final/payment` и `git push origin --delete final/payment`. В Bob выполни `git fetch --prune origin`: исчезнет origin/final/payment, но локальные ветки, если создавались, останутся.

Файлы из лабораторных не доказывают корректность настоящего приложения. Для рабочего кода на шагах проверки запускались бы тесты, сборка и при необходимости ручной сценарий.
